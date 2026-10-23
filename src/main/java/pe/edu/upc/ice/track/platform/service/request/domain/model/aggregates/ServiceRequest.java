package pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.Intervention;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceCompletedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceRequestAcceptedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceRequestCanceledEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceRequestCreatedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceRequestRejectedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceStartedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.TechnicianAssignedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * ServiceRequest aggregate root of the {@code service.request} bounded context.
 *
 * <p>An owner's request for a technician to service one of its refrigeration units, followed from
 * its creation to its completion or cancellation. The lifecycle is a finite state machine whose
 * matrix is carried by {@link ServiceStatus}; every business method checks the states it accepts,
 * records a {@link StatusTransition} in the append-only {@code history} and registers the matching
 * domain event.</p>
 *
 * <p>Owner, unit, site and technician are held by identifier only. They live in the
 * {@code profiles} and {@code assets} contexts, and the application layer checks them through its
 * outbound ACL services before any of them reaches this aggregate.</p>
 *
 * <p><strong>Failures.</strong> Only JDK exceptions are thrown, as everywhere in the platform's
 * domain: {@link IllegalArgumentException} when a value is invalid or the actor is not the party
 * the operation belongs to, and {@link IllegalStateException} when the current status does not
 * allow the operation. The application layer only hands an operation to this aggregate after
 * scoping the request to its caller with {@link #isOwnedBy} or {@link #isAssignedTo}, so for a REST
 * caller the actor check is a last line of defense: a non-party is answered 404 before it is
 * reached.</p>
 *
 * <p><strong>Idempotency.</strong> A technician's device may work offline and retry. Starting
 * again with the same device timestamp, and registering again an intervention with the same
 * {@code clientGeneratedId}, are answered as no-ops instead of conflicts, without a second
 * transition or event.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code ServiceRequestPersistenceEntity}.</p>
 */
@Getter
public class ServiceRequest extends AbstractDomainAggregateRoot<ServiceRequest> {

  /** Maximum number of characters the description may hold, matching the column width. */
  public static final int MAX_DESCRIPTION_LENGTH = 1000;

  /**
   * How far into the future a timestamp reported by a technician's device may lie. Device clocks
   * drift, so a few minutes are tolerated; anything beyond is a wrong clock rather than drift, and
   * would make the duration of the intervention meaningless.
   */
  public static final Duration MAX_DEVICE_CLOCK_SKEW = Duration.ofMinutes(5);

  private final ServiceRequestId id;
  private final OwnerId ownerId;
  private final EquipmentId equipmentId;
  private final SiteId siteId;
  private TechnicianProfileId technicianId;
  private final ServiceType type;
  private final ServicePriority priority;
  private final String description;
  private ServiceStatus status;
  private final Instant createdAt;
  private Instant assignedAt;
  private Instant startedAt;
  private Instant completedAt;
  private Instant canceledAt;
  private String cancelReason;

  /**
   * Optimistic concurrency token assigned by persistence, {@code null} until the request is first
   * saved. Opaque to the domain: it only travels so that a write based on a stale copy - an owner
   * canceling while the technician starts, for instance - is rejected instead of silently
   * overwriting the other one.
   */
  private final Long version;

  private final List<StatusTransition> history;
  private Intervention intervention;

  /**
   * Reconstitutes a service request, typically from persistence.
   *
   * <p>New requests must be created through {@link #create}.</p>
   *
   * @param id           the persistence identity, or {@code null} for a request not yet persisted
   * @param ownerId      the owner who created the request; required
   * @param equipmentId  the unit to service; required
   * @param siteId       the site the unit is installed at; required
   * @param technicianId the assigned technician, may be {@code null}
   * @param type         the kind of work; required
   * @param priority     the urgency; required
   * @param description  what the owner needs; required, at most {@value #MAX_DESCRIPTION_LENGTH}
   *                     characters
   * @param status       the current status; required
   * @param createdAt    when the request was created; required
   * @param assignedAt   when the current technician was assigned, may be {@code null}
   * @param startedAt    when the work started, may be {@code null}
   * @param completedAt  when the work finished, may be {@code null}
   * @param canceledAt   when the owner canceled, may be {@code null}
   * @param cancelReason why the owner canceled, may be {@code null}
   * @param version      the optimistic concurrency token, may be {@code null}
   * @param history      the recorded transitions, oldest first; may be {@code null} or empty
   * @param intervention the registered intervention, may be {@code null}
   * @throws IllegalArgumentException when a required value is missing or invalid
   */
  public ServiceRequest(ServiceRequestId id, OwnerId ownerId, EquipmentId equipmentId, SiteId siteId,
                        TechnicianProfileId technicianId, ServiceType type, ServicePriority priority,
                        String description, ServiceStatus status, Instant createdAt, Instant assignedAt,
                        Instant startedAt, Instant completedAt, Instant canceledAt, String cancelReason,
                        Long version, List<StatusTransition> history, Intervention intervention) {
    this.id = id;
    this.ownerId = requireValue(ownerId, "ownerId");
    this.equipmentId = requireValue(equipmentId, "equipmentId");
    if (siteId == null || siteId.siteId() == null) {
      throw new IllegalArgumentException("Service request siteId must not be null");
    }
    this.siteId = siteId;
    this.technicianId = technicianId;
    this.type = requireValue(type, "type");
    this.priority = requireValue(priority, "priority");
    this.description = validatedDescription(description);
    this.status = requireValue(status, "status");
    this.createdAt = requireValue(createdAt, "createdAt");
    this.assignedAt = assignedAt;
    this.startedAt = startedAt;
    this.completedAt = completedAt;
    this.canceledAt = canceledAt;
    this.cancelReason = cancelReason;
    this.version = version;
    this.history = new ArrayList<>(history == null ? List.of() : history);
    this.intervention = intervention;
  }

  /**
   * Creates a new service request, waiting for a technician.
   *
   * <p>The request starts {@link ServiceStatus#PENDING} with no technician, and its history opens
   * with the creation transition, attributed to the owner.</p>
   *
   * @param ownerId     the owner who creates the request; required
   * @param equipmentId the unit to service, already verified to belong to the owner; required
   * @param siteId      the site the unit is installed at; required
   * @param type        the kind of work; required
   * @param priority    the urgency; required
   * @param description what the owner needs; required
   * @return the new request, not yet persisted
   * @throws IllegalArgumentException when a value is missing or invalid
   */
  public static ServiceRequest create(OwnerId ownerId, EquipmentId equipmentId, SiteId siteId, ServiceType type,
                                      ServicePriority priority, String description) {
    var now = currentInstant();
    var request = new ServiceRequest(null, ownerId, equipmentId, siteId, null, type, priority, description,
        ServiceStatus.PENDING, now, null, null, null, null, null, null, List.of(), null);
    request.history.add(StatusTransition.of(null, ServiceStatus.PENDING, ownerId.ownerId(), now, null, null));
    return request;
  }

  /**
   * Signals that this request has just been created and persisted.
   *
   * <p>Called by the repository adapter once the persistence identity is available, since the
   * creation event carries it.</p>
   */
  public void onCreated() {
    if (id == null) {
      throw new IllegalStateException("A service request must be persisted before its creation is announced");
    }
    registerDomainEvent(ServiceRequestCreatedEvent.from(this));
  }

  /**
   * Assigns a technician, replacing the current one if any.
   *
   * <p>Allowed while the request is {@link ServiceStatus#PENDING} - with or without a technician -
   * or {@link ServiceStatus#ACCEPTED}. Either way the request goes back to {@code PENDING}: the new
   * technician still has to accept it.</p>
   *
   * @param newTechnicianId the technician to assign, already verified to be eligible; required
   * @param actorOwnerId    the owner performing the assignment; must be the request's owner
   * @throws IllegalArgumentException when the actor is not the owner or the technician is missing
   * @throws IllegalStateException    when the status is neither {@code PENDING} nor {@code ACCEPTED}
   */
  public void assignTechnician(TechnicianProfileId newTechnicianId, OwnerId actorOwnerId) {
    requireOwner(actorOwnerId);
    requireValue(newTechnicianId, "technicianId");
    ensureStatusIn("assign a technician to", ServiceStatus.PENDING, ServiceStatus.ACCEPTED);
    var previousTechnicianId = technicianId;
    var now = currentInstant();
    moveTo(ServiceStatus.PENDING, actorOwnerId.ownerId(), now, null, newTechnicianId);
    this.technicianId = newTechnicianId;
    this.assignedAt = now;
    registerDomainEvent(TechnicianAssignedEvent.from(this, previousTechnicianId));
  }

  /**
   * Accepts the request on behalf of the assigned technician.
   *
   * @param actorTechnicianId the technician accepting; must be the assigned one
   * @throws IllegalArgumentException when the actor is not the assigned technician
   * @throws IllegalStateException    when the request is not {@code PENDING}
   */
  public void accept(TechnicianProfileId actorTechnicianId) {
    requireAssignedTechnician(actorTechnicianId);
    ensureStatusIn("accept", ServiceStatus.PENDING);
    var now = currentInstant();
    moveTo(ServiceStatus.ACCEPTED, actorTechnicianId.technicianProfileId(), now, null, technicianId);
    registerDomainEvent(ServiceRequestAcceptedEvent.from(this, now));
  }

  /**
   * Rejects the request on behalf of the assigned technician.
   *
   * <p>The request stays {@code PENDING} and loses its technician, so the owner can assign another
   * one. The transition records who rejected and why.</p>
   *
   * @param actorTechnicianId the technician rejecting; must be the assigned one
   * @param reason            why the technician rejects; required
   * @throws IllegalArgumentException when the actor is not the assigned technician or the reason is
   *                                  missing
   * @throws IllegalStateException    when the request is not {@code PENDING}
   */
  public void reject(TechnicianProfileId actorTechnicianId, String reason) {
    requireAssignedTechnician(actorTechnicianId);
    ensureStatusIn("reject", ServiceStatus.PENDING);
    var validatedReason = requiredReason(reason, "rejection");
    var now = currentInstant();
    var rejectingTechnicianId = technicianId;
    moveTo(ServiceStatus.PENDING, actorTechnicianId.technicianProfileId(), now, validatedReason, rejectingTechnicianId);
    this.technicianId = null;
    this.assignedAt = null;
    registerDomainEvent(ServiceRequestRejectedEvent.from(this, rejectingTechnicianId, validatedReason, now));
  }

  /**
   * Starts the work on behalf of the assigned technician.
   *
   * <p>The start instant is the one the technician's device reports, so that work begun offline is
   * dated when it happened, or the server clock when the device reports none. Starting again an
   * {@code IN_PROGRESS} request with the very same device instant is a replay and is ignored.</p>
   *
   * @param actorTechnicianId the technician starting; must be the assigned one
   * @param deviceStartedAt   when the work started according to the device, may be {@code null}
   * @throws IllegalArgumentException when the actor is not the assigned technician or the instant
   *                                  lies in the future
   * @throws IllegalStateException    when the request is not {@code ACCEPTED} and the call is not a
   *                                  replay
   */
  public void start(TechnicianProfileId actorTechnicianId, Instant deviceStartedAt) {
    requireAssignedTechnician(actorTechnicianId);
    var reportedStartedAt = deviceStartedAt == null ? null : deviceStartedAt.truncatedTo(ChronoUnit.MICROS);
    if (status == ServiceStatus.IN_PROGRESS && reportedStartedAt != null && reportedStartedAt.equals(this.startedAt)) {
      return;
    }
    ensureStatusIn("start", ServiceStatus.ACCEPTED);
    var now = currentInstant();
    var effectiveStartedAt = reportedStartedAt == null ? now : notInTheFuture(reportedStartedAt, now, "startedAt");
    moveTo(ServiceStatus.IN_PROGRESS, actorTechnicianId.technicianProfileId(), effectiveStartedAt, null, technicianId);
    this.startedAt = effectiveStartedAt;
    registerDomainEvent(ServiceStartedEvent.from(this));
  }

  /**
   * Registers the technical report of the work and completes the request.
   *
   * <p>Idempotent on {@code clientGeneratedId}: when this request already holds the intervention
   * that identifier names, that intervention is returned as is - no new transition, no new event -
   * so a device retrying a submission it never saw acknowledged gets the same answer again. Any
   * other identifier on a request that is no longer {@code IN_PROGRESS} is a conflict.</p>
   *
   * @param actorTechnicianId the technician reporting; must be the assigned one
   * @param clientGeneratedId the identifier the device generated for this report; required
   * @param diagnosis         what the technician found; required
   * @param actionsTaken      what the technician did; required
   * @param spareParts        the spare parts used, may be {@code null} or empty
   * @param photoEvidence     references to the photos taken, may be {@code null} or empty
   * @param deviceCompletedAt when the work finished according to the device, may be {@code null}
   * @return the registered intervention, or the one already registered for that identifier
   * @throws IllegalArgumentException when the actor is not the assigned technician or the report
   *                                  is invalid
   * @throws IllegalStateException    when the request is not {@code IN_PROGRESS} and the call is not
   *                                  a replay
   */
  public Intervention registerIntervention(TechnicianProfileId actorTechnicianId, UUID clientGeneratedId,
                                           String diagnosis, String actionsTaken, List<String> spareParts,
                                           List<String> photoEvidence, Instant deviceCompletedAt) {
    requireValue(clientGeneratedId, "clientGeneratedId");
    requireAssignedTechnician(actorTechnicianId);
    if (hasInterventionIdentifiedBy(clientGeneratedId)) {
      return intervention;
    }
    ensureStatusIn("register an intervention for", ServiceStatus.IN_PROGRESS);
    var now = currentInstant();
    // Without a device instant the server clock is used, but never earlier than the start: a start
    // reported by a device whose clock runs slightly ahead would otherwise yield a negative duration.
    var effectiveCompletedAt = deviceCompletedAt == null
        ? latestOf(now, startedAt)
        : notInTheFuture(deviceCompletedAt.truncatedTo(ChronoUnit.MICROS), now, "completedAt");
    var registered = new Intervention(clientGeneratedId, technicianId, equipmentId, diagnosis, actionsTaken,
        spareParts, photoEvidence, startedAt, effectiveCompletedAt);
    moveTo(ServiceStatus.COMPLETED, actorTechnicianId.technicianProfileId(), effectiveCompletedAt, null, technicianId);
    this.intervention = registered;
    this.completedAt = effectiveCompletedAt;
    registerDomainEvent(ServiceCompletedEvent.from(this));
    return registered;
  }

  /**
   * Cancels the request on behalf of its owner.
   *
   * <p>Only allowed before the work starts: once a technician is on site the request has to be
   * completed, and a completed or canceled request is closed.</p>
   *
   * @param actorOwnerId the owner canceling; must be the request's owner
   * @param reason       why the owner cancels; required
   * @throws IllegalArgumentException when the actor is not the owner or the reason is missing
   * @throws IllegalStateException    when the request is neither {@code PENDING} nor {@code ACCEPTED}
   */
  public void cancel(OwnerId actorOwnerId, String reason) {
    requireOwner(actorOwnerId);
    ensureStatusIn("cancel", ServiceStatus.PENDING, ServiceStatus.ACCEPTED);
    var validatedReason = requiredReason(reason, "cancellation");
    var now = currentInstant();
    moveTo(ServiceStatus.CANCELED, actorOwnerId.ownerId(), now, validatedReason, technicianId);
    this.canceledAt = now;
    this.cancelReason = validatedReason;
    registerDomainEvent(ServiceRequestCanceledEvent.from(this));
  }

  /**
   * Returns the recorded transitions, oldest first.
   *
   * @return a read-only view of the history
   */
  public List<StatusTransition> getHistory() {
    return Collections.unmodifiableList(history);
  }

  /**
   * Tells whether a technician is assigned and still engaged with the request.
   *
   * @return {@code true} when a technician is assigned and the status is {@code PENDING},
   *         {@code ACCEPTED} or {@code IN_PROGRESS}
   */
  public boolean hasActiveTechnician() {
    return technicianId != null && status.isActive();
  }

  /**
   * Tells whether this request was created by the given owner.
   *
   * <p>The rule behind every owner-scoped read and write of this context.</p>
   *
   * @param candidate the owner to test
   * @return {@code true} when the identifiers match
   */
  public boolean isOwnedBy(OwnerId candidate) {
    return ownerId.equals(candidate);
  }

  /**
   * Tells whether this request is currently assigned to the given technician.
   *
   * <p>The rule behind every technician-scoped read and write of this context. A technician who
   * rejected the request is no longer assigned to it.</p>
   *
   * @param candidate the technician to test
   * @return {@code true} when a technician is assigned and the identifiers match
   */
  public boolean isAssignedTo(TechnicianProfileId candidate) {
    return technicianId != null && technicianId.equals(candidate);
  }

  /**
   * Tells whether this request already holds the intervention a client identifier names.
   *
   * @param clientGeneratedId the identifier to test
   * @return {@code true} when an intervention is registered under that identifier
   */
  public boolean hasInterventionIdentifiedBy(UUID clientGeneratedId) {
    return intervention != null && intervention.isIdentifiedBy(clientGeneratedId);
  }

  /**
   * Records a move to a new status, after checking it against the transition matrix.
   *
   * <p>Called before any other field is changed, so a refused move leaves the request untouched.</p>
   */
  private void moveTo(ServiceStatus target, Long actorId, Instant occurredAt, String reason,
                      TechnicianProfileId concernedTechnicianId) {
    if (!status.canTransitionTo(target)) {
      throw new IllegalStateException(
          "Illegal service request status transition from %s to %s; allowed targets are %s"
              .formatted(status, target, status.allowedTransitions()));
    }
    history.add(StatusTransition.of(status, target, actorId, occurredAt, reason, concernedTechnicianId));
    this.status = target;
  }

  private void ensureStatusIn(String operation, ServiceStatus... acceptedStatuses) {
    for (var accepted : acceptedStatuses) {
      if (status == accepted) {
        return;
      }
    }
    throw new IllegalStateException("Cannot %s a service request in status %s; it must be one of %s"
        .formatted(operation, status, Arrays.toString(acceptedStatuses)));
  }

  private void requireOwner(OwnerId actorOwnerId) {
    if (!isOwnedBy(actorOwnerId)) {
      throw new IllegalArgumentException(
          "Only the owner who created the service request may perform this operation");
    }
  }

  private void requireAssignedTechnician(TechnicianProfileId actorTechnicianId) {
    if (!isAssignedTo(actorTechnicianId)) {
      throw new IllegalArgumentException(
          "Only the technician assigned to the service request may perform this operation");
    }
  }

  /**
   * Reads the server clock at the precision PostgreSQL stores, so that an instant compares equal to
   * itself once it has been persisted and read back.
   */
  private static Instant currentInstant() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  private static Instant notInTheFuture(Instant reported, Instant now, String field) {
    if (reported.isAfter(now.plus(MAX_DEVICE_CLOCK_SKEW))) {
      throw new IllegalArgumentException("Service request %s must not lie in the future".formatted(field));
    }
    return reported;
  }

  private static Instant latestOf(Instant first, Instant second) {
    return first.isAfter(second) ? first : second;
  }

  private static String requiredReason(String reason, String kind) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("A %s reason is required".formatted(kind));
    }
    var trimmed = reason.strip();
    if (trimmed.length() > StatusTransition.MAX_REASON_LENGTH) {
      throw new IllegalArgumentException(
          "The %s reason must not exceed %d characters".formatted(kind, StatusTransition.MAX_REASON_LENGTH));
    }
    return trimmed;
  }

  private static String validatedDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Service request description must not be null or blank");
    }
    var trimmed = description.strip();
    if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
      throw new IllegalArgumentException(
          "Service request description must not exceed %d characters".formatted(MAX_DESCRIPTION_LENGTH));
    }
    return trimmed;
  }

  private static <T> T requireValue(T value, String field) {
    if (value == null) {
      throw new IllegalArgumentException("Service request %s must not be null".formatted(field));
    }
    return value;
  }
}
