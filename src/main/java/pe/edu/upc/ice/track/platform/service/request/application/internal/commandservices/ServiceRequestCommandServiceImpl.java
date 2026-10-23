package pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ServiceRequestCommandService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AcceptServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AssignTechnicianCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CancelServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CreateServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegisterInterventionCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RejectServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.StartServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.InterventionRegistration;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service Request Command Service Implementation.
 *
 * <p>Every command starts by resolving the caller's username to the profile it acts as through the
 * {@link ExternalUserProfileService} ACL - an owner for creating, assigning and canceling, a
 * technician for accepting, rejecting, starting and completing - and then loads the request scoped
 * to that profile. A request created by another owner, or not assigned to the calling technician, is
 * reported exactly like a request that does not exist: the alternative would turn these endpoints
 * into an oracle for discovering which identifiers are real. An account holding no profile of the
 * required role is answered with {@code ACCESS_DENIED}.</p>
 *
 * <p>The domain reports its failures with JDK exceptions, translated here right around the domain
 * call: {@link IllegalStateException} into {@code 409 INVALID_STATUS_TRANSITION} and
 * {@link IllegalArgumentException} into {@code 400 VALIDATION_ERROR}. Persistence failures are not
 * translated: a version conflict or a unique violation has already marked the transaction
 * rollback-only, so it propagates to {@code GlobalExceptionHandler}, which answers
 * {@code 409 CONCURRENT_MODIFICATION_CONFLICT} or {@code 409 DUPLICATE_RESOURCE_CONFLICT}.</p>
 */
@Service
@Slf4j
public class ServiceRequestCommandServiceImpl implements ServiceRequestCommandService {

  private static final String SERVICE_REQUEST_RESOURCE = "Service_Request";
  private static final String EQUIPMENT_RESOURCE = "Equipment";
  private static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  private static final String TECHNICIAN_NOT_ELIGIBLE = "TECHNICIAN_NOT_ELIGIBLE";

  private final ServiceRequestRepository serviceRequestRepository;
  private final ExternalEquipmentService externalEquipmentService;
  private final ExternalUserProfileService externalUserProfileService;

  /**
   * Constructor
   *
   * @param serviceRequestRepository   The {@link ServiceRequestRepository} instance
   * @param externalEquipmentService   The {@link ExternalEquipmentService} instance
   * @param externalUserProfileService The {@link ExternalUserProfileService} instance
   */
  public ServiceRequestCommandServiceImpl(
      ServiceRequestRepository serviceRequestRepository,
      ExternalEquipmentService externalEquipmentService,
      ExternalUserProfileService externalUserProfileService) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.externalEquipmentService = externalEquipmentService;
    this.externalUserProfileService = externalUserProfileService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ServiceRequest, ApplicationError> handle(CreateServiceRequestCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId -> {
      // A unit that does not exist and a unit of another owner answer the same 404, so creating a
      // request against somebody else's unit teaches nothing.
      var equipment = externalEquipmentService.fetchOwnedEquipment(ownerId, command.equipmentId());
      if (equipment.isEmpty()) {
        return Result.failure(notReachableEquipment(command.equipmentId()));
      }
      var reference = equipment.get();
      return attempt(() -> ServiceRequest.create(
              new OwnerId(ownerId),
              new EquipmentId(reference.equipmentId()),
              new SiteId(reference.siteId()),
              command.type(),
              command.priority(),
              command.description()))
          .map(request -> persist("Created", request));
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ServiceRequest, ApplicationError> handle(AssignTechnicianCommand command) {
    return resolveOwnerId(command.username())
        .flatMap(ownerId -> ownedServiceRequest(command.serviceRequestId(), ownerId)
            .flatMap(request -> {
              if (!externalUserProfileService.isTechnicianEligibleForAssignment(command.technicianProfileId())) {
                return Result.failure(technicianNotEligible(command.technicianProfileId()));
              }
              return attempt(() -> {
                request.assignTechnician(new TechnicianProfileId(command.technicianProfileId()), new OwnerId(ownerId));
                return request;
              }).map(assigned -> persist("Assigned a technician to", assigned));
            }));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ServiceRequest, ApplicationError> handle(AcceptServiceRequestCommand command) {
    return resolveTechnicianId(command.username())
        .flatMap(technicianId -> assignedServiceRequest(command.serviceRequestId(), technicianId)
            .flatMap(request -> attempt(() -> {
              request.accept(new TechnicianProfileId(technicianId));
              return request;
            }).map(accepted -> persist("Accepted", accepted))));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ServiceRequest, ApplicationError> handle(RejectServiceRequestCommand command) {
    return resolveTechnicianId(command.username())
        .flatMap(technicianId -> assignedServiceRequest(command.serviceRequestId(), technicianId)
            .flatMap(request -> attempt(() -> {
              request.reject(new TechnicianProfileId(technicianId), command.reason());
              return request;
            }).map(rejected -> persist("Rejected", rejected))));
  }

  /**
   * Starts the work on a request assigned to the caller.
   *
   * <p>A replay - starting again with the device instant already recorded - leaves the aggregate
   * untouched, so saving it writes nothing and publishes nothing.</p>
   */
  @Override
  @Transactional
  public Result<ServiceRequest, ApplicationError> handle(StartServiceRequestCommand command) {
    return resolveTechnicianId(command.username())
        .flatMap(technicianId -> assignedServiceRequest(command.serviceRequestId(), technicianId)
            .flatMap(request -> attempt(() -> {
              request.start(new TechnicianProfileId(technicianId), command.startedAt());
              return request;
            }).map(started -> persist("Started", started))));
  }

  /**
   * Registers the intervention that completes a request assigned to the caller.
   *
   * <p>Idempotent on {@code clientGeneratedId}. When the request already holds the intervention the
   * identifier names, the submission is a replay: the stored intervention is returned and nothing
   * is written. When the identifier already names the intervention of <em>another</em> request, the
   * submission is refused with {@code 409 DUPLICATE_RESOURCE_CONFLICT}; the unique index on
   * {@code client_generated_id} settles the race this check cannot see, which surfaces through
   * {@code GlobalExceptionHandler} with the same code.</p>
   */
  @Override
  @Transactional
  public Result<InterventionRegistration, ApplicationError> handle(RegisterInterventionCommand command) {
    return resolveTechnicianId(command.username())
        .flatMap(technicianId -> assignedServiceRequest(command.serviceRequestId(), technicianId)
            .flatMap(request -> {
              var clientGeneratedId = command.clientGeneratedId();
              var replayed = request.hasInterventionIdentifiedBy(clientGeneratedId);
              if (!replayed && clientGeneratedId != null
                  && serviceRequestRepository.existsByInterventionClientGeneratedId(clientGeneratedId)) {
                return Result.failure(ApplicationError.duplicateResource(
                    "client_generated_id %s already identifies the intervention of another service request"
                        .formatted(clientGeneratedId)));
              }
              return attempt(() -> {
                request.registerIntervention(
                    new TechnicianProfileId(technicianId),
                    clientGeneratedId,
                    command.diagnosis(),
                    command.actionsTaken(),
                    command.spareParts(),
                    command.photoEvidence(),
                    command.completedAt());
                return request;
              }).map(completed -> replayed
                  ? new InterventionRegistration(completed, true)
                  : new InterventionRegistration(persist("Completed", completed), false));
            }));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ServiceRequest, ApplicationError> handle(CancelServiceRequestCommand command) {
    return resolveOwnerId(command.username())
        .flatMap(ownerId -> ownedServiceRequest(command.serviceRequestId(), ownerId)
            .flatMap(request -> attempt(() -> {
              request.cancel(new OwnerId(ownerId), command.reason());
              return request;
            }).map(canceled -> persist("Canceled", canceled))));
  }

  /**
   * Runs a domain operation, translating the JDK exceptions the domain reports its failures with.
   *
   * <p>Only the domain call runs inside; persisting the outcome happens afterward, outside this
   * translation, so a persistence failure is never mistaken for a status conflict.</p>
   *
   * @param operation the domain operation
   * @param <T>       the type of its outcome
   * @return the outcome; a 409 failure for an {@link IllegalStateException}, or a 400 failure for an
   *         {@link IllegalArgumentException}
   */
  private <T> Result<T, ApplicationError> attempt(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (IllegalStateException e) {
      return Result.failure(invalidStatusTransition(e.getMessage()));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(SERVICE_REQUEST_RESOURCE, e.getMessage()));
    }
  }

  /**
   * Saves a request and logs what happened to it.
   *
   * @param action  what was done, phrased as a past-tense verb
   * @param request the request to save
   * @return the saved request
   */
  private ServiceRequest persist(String action, ServiceRequest request) {
    var saved = serviceRequestRepository.save(request);
    log.info("{} service request {}; its status is now {}",
        action, saved.getId().serviceRequestId(), saved.getStatus());
    return saved;
  }

  /**
   * Resolves the caller's username to the owner it acts as.
   *
   * @param username username of the requesting caller
   * @return the owner identifier, or a 403 failure when the caller holds no owner profile
   */
  private Result<Long, ApplicationError> resolveOwnerId(String username) {
    return externalUserProfileService.fetchOwnerIdByUsername(username)
        .<Result<Long, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(noOwnerProfile()));
  }

  /**
   * Resolves the caller's username to the technician it acts as.
   *
   * @param username username of the requesting caller
   * @return the technician identifier, or a 403 failure when the caller holds no technician profile
   */
  private Result<Long, ApplicationError> resolveTechnicianId(String username) {
    return externalUserProfileService.fetchTechnicianIdByUsername(username)
        .<Result<Long, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(noTechnicianProfile()));
  }

  /**
   * Loads a request the caller may act on as an owner.
   *
   * @param serviceRequestId the request identifier
   * @param ownerId          the owner the caller acts as
   * @return the request, or a 404 failure when it does not exist or was created by another owner
   */
  private Result<ServiceRequest, ApplicationError> ownedServiceRequest(Long serviceRequestId, Long ownerId) {
    var owner = new OwnerId(ownerId);
    return findServiceRequest(serviceRequestId)
        .filter(request -> request.isOwnedBy(owner))
        .<Result<ServiceRequest, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(notReachableServiceRequest(serviceRequestId)));
  }

  /**
   * Loads a request the caller may act on as a technician.
   *
   * @param serviceRequestId the request identifier
   * @param technicianId     the technician the caller acts as
   * @return the request, or a 404 failure when it does not exist or is not assigned to the caller
   */
  private Result<ServiceRequest, ApplicationError> assignedServiceRequest(Long serviceRequestId, Long technicianId) {
    var technician = new TechnicianProfileId(technicianId);
    return findServiceRequest(serviceRequestId)
        .filter(request -> request.isAssignedTo(technician))
        .<Result<ServiceRequest, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(notReachableServiceRequest(serviceRequestId)));
  }

  /**
   * Loads a request whoever its parties are.
   *
   * @param serviceRequestId the request identifier, may be {@code null}
   * @return the request, or empty when the identifier is absent, not positive or unknown
   */
  private Optional<ServiceRequest> findServiceRequest(Long serviceRequestId) {
    if (serviceRequestId == null || serviceRequestId <= 0) {
      return Optional.empty();
    }
    return serviceRequestRepository.findById(new ServiceRequestId(serviceRequestId));
  }

  /**
   * Builds the failure reported when a request cannot be reached by the caller.
   *
   * @param serviceRequestId the request identifier that was requested
   * @return a 404 failure that never reveals whether the request exists for somebody else
   */
  private static ApplicationError notReachableServiceRequest(Long serviceRequestId) {
    return ApplicationError.notFound(SERVICE_REQUEST_RESOURCE, String.valueOf(serviceRequestId));
  }

  /**
   * Builds the failure reported when a request is opened against a unit the caller cannot reach.
   *
   * @param equipmentId the unit identifier that was requested
   * @return a 404 failure, the same {@code EQUIPMENT_NOT_FOUND} the {@code assets} context answers
   */
  private static ApplicationError notReachableEquipment(Long equipmentId) {
    return ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(equipmentId));
  }

  /**
   * Builds the failure reported when the status of the request does not allow the operation.
   *
   * @param reason the explanation reported by the domain
   * @return a 409 failure
   */
  private static ApplicationError invalidStatusTransition(String reason) {
    return new ApplicationError(INVALID_STATUS_TRANSITION, "Invalid service request status transition", reason);
  }

  /**
   * Builds the failure reported when the technician to assign is not ready to be assigned work.
   *
   * @param technicianProfileId the technician that was to be assigned
   * @return a 409 failure
   */
  private static ApplicationError technicianNotEligible(Long technicianProfileId) {
    return new ApplicationError(
        TECHNICIAN_NOT_ELIGIBLE,
        "Technician not eligible for assignment",
        "Technician %s is not ready to be assigned a service request".formatted(technicianProfileId));
  }

  /**
   * Builds the failure reported when the caller's account holds no owner profile.
   *
   * <p>Reported as access denied rather than as a missing request, exactly like {@code assets}
   * does: the caller is known, there is simply no owner behind it.</p>
   *
   * @return a 403 failure
   */
  private static ApplicationError noOwnerProfile() {
    return ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no service request");
  }

  /**
   * Builds the failure reported when the caller's account holds no technician profile.
   *
   * @return a 403 failure
   */
  private static ApplicationError noTechnicianProfile() {
    return ApplicationError.accessDenied(
        "The authenticated account has no technician profile, so no service request is assigned to it");
  }
}
