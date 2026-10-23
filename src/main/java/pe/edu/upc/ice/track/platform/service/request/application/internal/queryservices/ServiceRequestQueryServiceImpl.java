package pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.queryservices.ServiceRequestQueryService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestByIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListEquipmentInterventionsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListServiceRequestsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Application service that resolves service request read queries.
 *
 * <p>Every query names its caller by username, resolved through the
 * {@link ExternalUserProfileService} ACL to the owner or the technician it acts as before anything
 * is read. An owner reads the requests it
 * created; a technician reads the requests currently assigned to it. A caller that holds no profile
 * of its role reads nothing.</p>
 */
@Service
public class ServiceRequestQueryServiceImpl implements ServiceRequestQueryService {

  /**
   * Statuses in which a technician assigned to a request on a unit may read the unit's intervention
   * history: while it is about to service the unit, and once it has. A canceled request leaves no
   * such relationship behind.
   */
  private static final Set<ServiceStatus> TECHNICIAN_ENGAGED_STATUSES = Set.of(
      ServiceStatus.PENDING, ServiceStatus.ACCEPTED, ServiceStatus.IN_PROGRESS, ServiceStatus.COMPLETED);

  private final ServiceRequestRepository serviceRequestRepository;
  private final ExternalEquipmentService externalEquipmentService;
  private final ExternalUserProfileService externalUserProfileService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param serviceRequestRepository   service request repository port
   * @param externalEquipmentService   outbound ACL service, used to check the owner of a unit
   * @param externalUserProfileService outbound ACL service, used to resolve the caller to its profile
   */
  public ServiceRequestQueryServiceImpl(
      ServiceRequestRepository serviceRequestRepository,
      ExternalEquipmentService externalEquipmentService,
      ExternalUserProfileService externalUserProfileService) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.externalEquipmentService = externalEquipmentService;
    this.externalUserProfileService = externalUserProfileService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<ServiceRequest> handle(GetServiceRequestByIdQuery query) {
    if (query.serviceRequestId() == null || query.serviceRequestId() <= 0) {
      return Optional.empty();
    }
    var serviceRequestId = new ServiceRequestId(query.serviceRequestId());
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return externalUserProfileService.fetchTechnicianIdByUsername(query.username())
          .map(TechnicianProfileId::new)
          .flatMap(technicianId -> serviceRequestRepository.findById(serviceRequestId)
              .filter(request -> request.isAssignedTo(technicianId)));
    }
    return externalUserProfileService.fetchOwnerIdByUsername(query.username())
        .map(OwnerId::new)
        .flatMap(ownerId -> serviceRequestRepository.findById(serviceRequestId)
            .filter(request -> request.isOwnedBy(ownerId)));
  }

  /**
   * Lists one page of the caller's requests.
   *
   * <p>The scope - created by the owner, or assigned to the technician - is resolved from the caller
   * and handed to the repository as a mandatory restriction; the status filter can only narrow
   * it.</p>
   *
   * @param query the query naming the caller, the status filter and the requested page
   * @return the requested page, empty when the caller holds no profile of its role
   */
  @Override
  @Transactional(readOnly = true)
  public ServiceRequestPage handle(ListServiceRequestsQuery query) {
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return externalUserProfileService.fetchTechnicianIdByUsername(query.username())
          .map(technicianId -> serviceRequestRepository.findPageByTechnicianId(
              new TechnicianProfileId(technicianId), query.statuses(), query.page(), query.size()))
          .orElseGet(() -> ServiceRequestPage.empty(query.page(), query.size()));
    }
    return externalUserProfileService.fetchOwnerIdByUsername(query.username())
        .map(ownerId -> serviceRequestRepository.findPageByOwnerId(
            new OwnerId(ownerId), query.statuses(), query.page(), query.size()))
        .orElseGet(() -> ServiceRequestPage.empty(query.page(), query.size()));
  }

  /**
   * Lists the completed requests of a unit, once the caller is known to have a say on it.
   *
   * <p>An owner must own the unit, which is checked against {@code assets} through the
   * {@link ExternalEquipmentService}. A technician must be assigned to a request on the unit that is
   * still active or that it completed. Otherwise the unit is reported as unreachable, exactly like a
   * unit that does not exist.</p>
   *
   * @param query the query naming the unit and the caller
   * @return the completed requests, most recent first, or empty when the unit is out of reach
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<List<ServiceRequest>> handle(ListEquipmentInterventionsQuery query) {
    if (query.equipmentId() == null || query.equipmentId() <= 0) {
      return Optional.empty();
    }
    var equipmentId = new EquipmentId(query.equipmentId());
    var reachable = query.callerRole() == CallerRole.TECHNICIAN
        ? externalUserProfileService.fetchTechnicianIdByUsername(query.username())
            .filter(technicianId -> serviceRequestRepository.existsByEquipmentIdAndTechnicianIdAndStatusIn(
                equipmentId, new TechnicianProfileId(technicianId), TECHNICIAN_ENGAGED_STATUSES))
            .isPresent()
        : externalUserProfileService.fetchOwnerIdByUsername(query.username())
            .flatMap(ownerId -> externalEquipmentService.fetchOwnedEquipment(ownerId, query.equipmentId()))
            .isPresent();
    if (!reachable) {
      return Optional.empty();
    }
    return Optional.of(serviceRequestRepository.findCompletedByEquipmentId(equipmentId));
  }
}
