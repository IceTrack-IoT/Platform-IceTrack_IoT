package pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ServiceRequestCommandService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalMonitoringService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.tokens.EdgeGrantTokenService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AcceptServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AssignTechnicianCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CancelServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CompleteMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CreateServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RecordMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegenerateEdgeGrantCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegisterInterventionCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RejectServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.StartServiceRequestCommand;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.AcceptedServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.InterventionRegistration;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.IssuedEdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.EdgeGrantRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.time.Duration;
import java.time.Instant;
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
 *
 * <p><strong>Edge grants.</strong> Accepting a request issues an {@link EdgeGrant} to its
 * technician in the same transaction, and the plaintext token travels back once in the outcome.
 * Reassigning, canceling or completing the request revokes its unrevoked grants, also in the same
 * transaction, and the assigned technician may regenerate a lost grant while the request is
 * {@code ACCEPTED} or {@code IN_PROGRESS}. Grants live for
 * {@code service-request.edge-grant.ttl-hours} (24 by default).</p>
 *
 * <p><strong>Maintenance window links.</strong> The two link commands come from Monitoring and
 * Alerting's events after their transaction committed, so each runs in a transaction of its own and
 * reports a rejected link by logging it and answering {@code false} instead of throwing.</p>
 */
@Service
@Slf4j
public class ServiceRequestCommandServiceImpl implements ServiceRequestCommandService {

  private static final String SERVICE_REQUEST_RESOURCE = "Service_Request";
  private static final String EQUIPMENT_RESOURCE = "Equipment";
  private static final String ALERT_RESOURCE = "Alert";
  private static final String SOURCE_ALERT_ID_FIELD = "source_alert_id";
  private static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  private static final String TECHNICIAN_NOT_ELIGIBLE = "TECHNICIAN_NOT_ELIGIBLE";

  /** Revocation reasons recorded on edge grants. */
  static final String GRANT_REASSIGNED = "REASSIGNED";
  static final String GRANT_CANCELED = "CANCELED";
  static final String GRANT_COMPLETED = "COMPLETED";
  static final String GRANT_REGENERATED = "REGENERATED";

  private final ServiceRequestRepository serviceRequestRepository;
  private final ExternalEquipmentService externalEquipmentService;
  private final ExternalUserProfileService externalUserProfileService;
  private final ExternalMonitoringService externalMonitoringService;
  private final EdgeGrantRepository edgeGrantRepository;
  private final EdgeGrantTokenService edgeGrantTokenService;
  private final Duration edgeGrantTimeToLive;

  /**
   * Constructor
   *
   * @param serviceRequestRepository   The {@link ServiceRequestRepository} instance
   * @param externalEquipmentService   The {@link ExternalEquipmentService} instance
   * @param externalUserProfileService The {@link ExternalUserProfileService} instance
   * @param externalMonitoringService  The {@link ExternalMonitoringService} instance, used to resolve
   *                                   the alert a corrective request is opened for
   * @param edgeGrantRepository        The {@link EdgeGrantRepository} instance
   * @param edgeGrantTokenService      The {@link EdgeGrantTokenService} instance
   * @param edgeGrantTtlHours          how long an edge grant stays valid, in hours
   */
  public ServiceRequestCommandServiceImpl(
      ServiceRequestRepository serviceRequestRepository,
      ExternalEquipmentService externalEquipmentService,
      ExternalUserProfileService externalUserProfileService,
      ExternalMonitoringService externalMonitoringService,
      EdgeGrantRepository edgeGrantRepository,
      EdgeGrantTokenService edgeGrantTokenService,
      @Value("${service-request.edge-grant.ttl-hours:24}") long edgeGrantTtlHours) {
    if (edgeGrantTtlHours < 1) {
      throw new IllegalStateException("service-request.edge-grant.ttl-hours must be at least 1");
    }
    this.serviceRequestRepository = serviceRequestRepository;
    this.externalEquipmentService = externalEquipmentService;
    this.externalUserProfileService = externalUserProfileService;
    this.externalMonitoringService = externalMonitoringService;
    this.edgeGrantRepository = edgeGrantRepository;
    this.edgeGrantTokenService = edgeGrantTokenService;
    this.edgeGrantTimeToLive = Duration.ofHours(edgeGrantTtlHours);
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
      var sourceAlertFailure = checkSourceAlert(command.sourceAlertId(), ownerId, reference.equipmentId());
      if (sourceAlertFailure.isPresent()) {
        return Result.failure(sourceAlertFailure.get());
      }
      return attempt(() -> ServiceRequest.create(
              new OwnerId(ownerId),
              new EquipmentId(reference.equipmentId()),
              new SiteId(reference.siteId()),
              command.type(),
              command.priority(),
              command.description(),
              command.sourceAlertId()))
          .map(request -> persist("Created", request));
    });
  }

  /**
   * Checks the alert a new request is opened for, when one is named.
   *
   * <p>An alert that does not exist and an alert raised on a unit of another owner answer the same
   * {@code 404 ALERT_NOT_FOUND}, so the check teaches nothing about other owners' alerts. An alert of
   * the caller raised on another of its units is a mistake in the request rather than an absence:
   * {@code 400 VALIDATION_ERROR}. The status of the alert does not matter, since the repair may come
   * after the alert closed.</p>
   *
   * @param sourceAlertId the alert named by the request, may be {@code null}
   * @param ownerId       the owner the caller acts as
   * @param equipmentId   the unit the request is opened against, already verified to be the owner's
   * @return the failure to report, or empty when no alert is named or the alert fits
   */
  private Optional<ApplicationError> checkSourceAlert(Long sourceAlertId, Long ownerId, Long equipmentId) {
    if (sourceAlertId == null) {
      return Optional.empty();
    }
    var alertEquipmentId = externalMonitoringService.fetchAlertEquipmentId(sourceAlertId)
        .filter(alertUnit -> externalEquipmentService.isEquipmentOwnedBy(ownerId, alertUnit));
    if (alertEquipmentId.isEmpty()) {
      return Optional.of(ApplicationError.notFound(ALERT_RESOURCE, String.valueOf(sourceAlertId)));
    }
    if (!alertEquipmentId.get().equals(equipmentId)) {
      return Optional.of(ApplicationError.validationError(
          SOURCE_ALERT_ID_FIELD, "Field source_alert_id: belongs to another unit"));
    }
    return Optional.empty();
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
              }).map(assigned -> {
                var saved = persist("Assigned a technician to", assigned);
                // The outgoing technician, if the request had been accepted, loses gateway access.
                revokeEdgeGrants(saved, GRANT_REASSIGNED);
                return saved;
              });
            }));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<AcceptedServiceRequest, ApplicationError> handle(AcceptServiceRequestCommand command) {
    return resolveTechnicianId(command.username())
        .flatMap(technicianId -> assignedServiceRequest(command.serviceRequestId(), technicianId)
            .flatMap(request -> attempt(() -> {
              request.accept(new TechnicianProfileId(technicianId));
              return request;
            }).map(accepted -> {
              var saved = persist("Accepted", accepted);
              return new AcceptedServiceRequest(saved, issueEdgeGrant(saved, GRANT_REGENERATED));
            })));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<IssuedEdgeGrant, ApplicationError> handle(RegenerateEdgeGrantCommand command) {
    return resolveTechnicianId(command.username()).flatMap(technicianId -> {
      var request = findServiceRequest(command.serviceRequestId());
      if (request.isEmpty()) {
        return Result.failure(notReachableServiceRequest(command.serviceRequestId()));
      }
      // Unlike the other technician commands, a request assigned to somebody else is answered 403:
      // the caller is a technician asking for gateway access it does not hold.
      if (!request.get().isAssignedTo(new TechnicianProfileId(technicianId))) {
        return Result.failure(ApplicationError.accessDenied(
            "Only the technician assigned to the service request may regenerate its edge grant"));
      }
      var status = request.get().getStatus();
      if (status != ServiceStatus.ACCEPTED && status != ServiceStatus.IN_PROGRESS) {
        return Result.failure(invalidStatusTransition(
            "An edge grant can only be regenerated for an ACCEPTED or IN_PROGRESS service request, not %s"
                .formatted(status)));
      }
      return Result.success(issueEdgeGrant(request.get(), GRANT_REGENERATED));
    });
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
              }).map(completed -> {
                if (replayed) {
                  return new InterventionRegistration(completed, true);
                }
                var saved = persist("Completed", completed);
                revokeEdgeGrants(saved, GRANT_COMPLETED);
                return new InterventionRegistration(saved, false);
              });
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
            }).map(canceled -> {
              var saved = persist("Canceled", canceled);
              revokeEdgeGrants(saved, GRANT_CANCELED);
              return saved;
            })));
  }

  // inherited javadoc
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(RecordMaintenanceWindowLinkCommand command) {
    var request = findServiceRequest(command.serviceRequestId());
    if (request.isEmpty()) {
      log.warn("Maintenance window {} names service request {}, which does not exist; not linked",
          command.windowId(), command.serviceRequestId());
      return false;
    }
    var serviceRequest = request.get();
    if (!serviceRequest.getEquipmentId().equals(new EquipmentId(command.equipmentId()))) {
      log.warn("Maintenance window {} of equipment {} names service request {} of equipment {}; not linked",
          command.windowId(), command.equipmentId(), command.serviceRequestId(),
          serviceRequest.getEquipmentId().equipmentId());
      return false;
    }
    final boolean linked;
    try {
      linked = serviceRequest.linkMaintenanceWindow(command.windowId(), command.origin(),
          command.technicianProfileId(), command.windowStartedAt(), command.attributedAt());
    } catch (IllegalStateException | IllegalArgumentException e) {
      log.warn("Maintenance window {} not linked to service request {}: {}",
          command.windowId(), command.serviceRequestId(), e.getMessage());
      return false;
    }
    if (!linked) {
      log.info("Maintenance window {} is already linked to service request {}",
          command.windowId(), command.serviceRequestId());
      return false;
    }
    persist("Linked maintenance window %s to".formatted(command.windowId()), serviceRequest);
    return true;
  }

  // inherited javadoc
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(CompleteMaintenanceWindowLinkCommand command) {
    var request = findServiceRequest(command.serviceRequestId());
    if (request.isEmpty()) {
      log.warn("Closed maintenance window {} names service request {}, which does not exist",
          command.windowId(), command.serviceRequestId());
      return false;
    }
    var serviceRequest = request.get();
    final boolean completed;
    try {
      completed = serviceRequest.completeMaintenanceWindowLink(
          command.windowId(), command.endedAt(), command.closureReason());
    } catch (IllegalArgumentException e) {
      log.warn("Link of maintenance window {} on service request {} not completed: {}",
          command.windowId(), command.serviceRequestId(), e.getMessage());
      return false;
    }
    if (!completed) {
      log.info("Service request {} holds no open link to maintenance window {}; nothing to complete",
          command.serviceRequestId(), command.windowId());
      return false;
    }
    persist("Completed the link of maintenance window %s on".formatted(command.windowId()), serviceRequest);
    return true;
  }

  /**
   * Issues an edge grant to the technician of a request, revoking any grant of the request that is
   * still unrevoked first, so a request never holds two live grants.
   *
   * @param request          the persisted request, assigned to a technician
   * @param previousReason   the revocation reason recorded on a grant being replaced
   * @return the issued grant, carrying its plaintext token
   */
  private IssuedEdgeGrant issueEdgeGrant(ServiceRequest request, String previousReason) {
    revokeEdgeGrants(request, previousReason);
    var token = edgeGrantTokenService.generate();
    var now = Instant.now();
    var grant = edgeGrantRepository.save(EdgeGrant.issue(
        request.getId().serviceRequestId(),
        request.getTechnicianId().technicianProfileId(),
        request.getEquipmentId().equipmentId(),
        token.digest(),
        now,
        now.plus(edgeGrantTimeToLive)));
    log.info("Issued edge grant {} for service request {} to technician {}, valid until {}",
        grant.getId(), grant.getServiceRequestId(), grant.getTechnicianProfileId(), grant.getExpiresAt());
    return new IssuedEdgeGrant(grant, token.plainText());
  }

  /**
   * Revokes every unrevoked edge grant of a request.
   *
   * @param request the persisted request
   * @param reason  why the grants end
   */
  private void revokeEdgeGrants(ServiceRequest request, String reason) {
    var now = Instant.now();
    for (var grant : edgeGrantRepository.findUnrevokedByServiceRequestId(request.getId().serviceRequestId())) {
      grant.revoke(reason, now);
      edgeGrantRepository.save(grant);
      log.info("Revoked edge grant {} of service request {} ({})", grant.getId(), grant.getServiceRequestId(), reason);
    }
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
