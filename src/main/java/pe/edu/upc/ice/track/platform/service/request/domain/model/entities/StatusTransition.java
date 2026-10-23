package pe.edu.upc.ice.track.platform.service.request.domain.model.entities;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;

import java.time.Instant;

/**
 * One entry of the status history of a {@code ServiceRequest}.
 *
 * <p>History is append-only: a transition is never changed or removed once recorded, which is why
 * every field is final. The first entry of every request is its creation, the only transition
 * without a {@code fromStatus}.</p>
 *
 * <p>{@code technicianId} is the technician the transition concerns, which is not always the one
 * the request holds afterward: a rejection records the technician who rejected, although the
 * request is left without one.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code StatusTransitionPersistenceEntity}.</p>
 */
@Getter
public class StatusTransition {

  /** Maximum number of characters a reason may hold, matching the {@code reason} column width. */
  public static final int MAX_REASON_LENGTH = 500;

  private final Long id;
  private final ServiceStatus fromStatus;
  private final ServiceStatus toStatus;
  private final Long actorId;
  private final Instant occurredAt;
  private final String reason;
  private final TechnicianProfileId technicianId;

  /**
   * Reconstitutes a transition, typically from persistence.
   *
   * @param id           the persistence identity, or {@code null} for a transition not yet persisted
   * @param fromStatus   the status before the transition, {@code null} only for the creation
   * @param toStatus     the status after the transition; required
   * @param actorId      the profile identifier of the owner or technician who acted; required
   * @param occurredAt   when the transition happened; required
   * @param reason       why it happened, may be {@code null}; a blank value is stored as {@code null}
   * @param technicianId the technician the transition concerns, may be {@code null}
   * @throws IllegalArgumentException when a required value is missing or the reason is too long
   */
  public StatusTransition(Long id, ServiceStatus fromStatus, ServiceStatus toStatus, Long actorId,
                          Instant occurredAt, String reason, TechnicianProfileId technicianId) {
    if (toStatus == null) {
      throw new IllegalArgumentException("Status transition target must not be null");
    }
    if (actorId == null) {
      throw new IllegalArgumentException("Status transition actor must not be null");
    }
    if (occurredAt == null) {
      throw new IllegalArgumentException("Status transition instant must not be null");
    }
    this.id = id;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.actorId = actorId;
    this.occurredAt = occurredAt;
    this.reason = normalizedReason(reason);
    this.technicianId = technicianId;
  }

  /**
   * Records a new, not yet persisted, transition.
   *
   * @param fromStatus   the status before the transition, {@code null} only for the creation
   * @param toStatus     the status after the transition; required
   * @param actorId      the profile identifier of the owner or technician who acted; required
   * @param occurredAt   when the transition happened; required
   * @param reason       why it happened, may be {@code null}
   * @param technicianId the technician the transition concerns, may be {@code null}
   * @return the transition
   */
  public static StatusTransition of(ServiceStatus fromStatus, ServiceStatus toStatus, Long actorId,
                                    Instant occurredAt, String reason, TechnicianProfileId technicianId) {
    return new StatusTransition(null, fromStatus, toStatus, actorId, occurredAt, reason, technicianId);
  }

  /**
   * Tells whether this transition is the creation of the request.
   *
   * @return {@code true} when there was no status before it
   */
  public boolean isCreation() {
    return fromStatus == null;
  }

  private static String normalizedReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    var trimmed = reason.strip();
    if (trimmed.length() > MAX_REASON_LENGTH) {
      throw new IllegalArgumentException(
          "Status transition reason must not exceed %d characters".formatted(MAX_REASON_LENGTH));
    }
    return trimmed;
  }
}
