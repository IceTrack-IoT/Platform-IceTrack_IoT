package pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.queryservices.ServiceRequestQueryService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestByIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestDisplayDetailsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListEquipmentInterventionsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListServiceRequestsQuery;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestDisplayDetails;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final ReviewRepository reviewRepository;
  private final ExternalEquipmentService externalEquipmentService;
  private final ExternalUserProfileService externalUserProfileService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param serviceRequestRepository   service request repository port
   * @param reviewRepository           review repository port, used to tell which requests were reviewed
   * @param externalEquipmentService   outbound ACL service, used to check the owner of a unit and to
   *                                   name units and sites
   * @param externalUserProfileService outbound ACL service, used to resolve the caller to its profile
   *                                   and to name technicians
   */
  public ServiceRequestQueryServiceImpl(
      ServiceRequestRepository serviceRequestRepository,
      ReviewRepository reviewRepository,
      ExternalEquipmentService externalEquipmentService,
      ExternalUserProfileService externalUserProfileService) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.reviewRepository = reviewRepository;
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
   * and handed to the repository as a mandatory restriction; the filters can only narrow it, so a
   * unit, site or alert of another owner yields an empty page rather than an error.</p>
   *
   * @param query the query naming the caller, the filters and the requested page
   * @return the requested page, empty when the caller holds no profile of its role
   */
  @Override
  @Transactional(readOnly = true)
  public ServiceRequestPage handle(ListServiceRequestsQuery query) {
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return externalUserProfileService.fetchTechnicianIdByUsername(query.username())
          .map(technicianId -> serviceRequestRepository.findPageByTechnicianId(
              new TechnicianProfileId(technicianId), query.filter(), query.page(), query.size()))
          .orElseGet(() -> ServiceRequestPage.empty(query.page(), query.size()));
    }
    return externalUserProfileService.fetchOwnerIdByUsername(query.username())
        .map(ownerId -> serviceRequestRepository.findPageByOwnerId(
            new OwnerId(ownerId), query.filter(), query.page(), query.size()))
        .orElseGet(() -> ServiceRequestPage.empty(query.page(), query.size()));
  }

  /**
   * Resolves the names a set of requests is shown with, and optionally their reviews.
   *
   * <p>Units and sites are named from their owner's directory, read once per distinct owner among
   * the requests - one owner for an owner's listing, a few for a technician's - instead of once per
   * request. Technicians are named in one call, and reviews in one read.</p>
   *
   * @param query the requests, already scoped to the caller
   * @return the details
   */
  @Override
  @Transactional(readOnly = true)
  public ServiceRequestDisplayDetails handle(GetServiceRequestDisplayDetailsQuery query) {
    var requests = query.serviceRequests();
    if (requests.isEmpty()) {
      return ServiceRequestDisplayDetails.NONE;
    }
    var equipmentNames = new HashMap<Long, String>();
    var siteNames = new HashMap<Long, String>();
    requests.stream()
        .map(request -> request.getOwnerId().ownerId())
        .distinct()
        .forEach(ownerId -> {
          equipmentNames.putAll(externalEquipmentService.fetchEquipmentNamesByOwner(ownerId));
          siteNames.putAll(externalEquipmentService.fetchSiteNamesByOwner(ownerId));
        });
    var technicianIds = requests.stream()
        .map(ServiceRequest::getTechnicianId)
        .filter(Objects::nonNull)
        .map(TechnicianProfileId::technicianProfileId)
        .collect(Collectors.toSet());
    var technicianNames = externalUserProfileService.fetchTechnicianFullNames(technicianIds);
    Set<Long> reviewed = query.includeReviews()
        ? reviewRepository.findReviewedServiceRequestIds(requests.stream()
            .filter(request -> request.getId() != null)
            .map(request -> request.getId().serviceRequestId())
            .toList())
        : Set.of();
    return new ServiceRequestDisplayDetails(equipmentNames, siteNames, technicianNames, reviewed);
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
