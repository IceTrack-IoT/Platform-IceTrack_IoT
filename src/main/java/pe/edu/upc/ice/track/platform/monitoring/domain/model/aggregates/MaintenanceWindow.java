package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowClosedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowOpenedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowPromotedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * MaintenanceWindow aggregate root of the {@code monitoring} bounded context.
 *
 * <p>The period during which a unit is being worked on, as reported by its edge gateway. While a
 * window covers an instant, readings taken at that instant are flagged as maintenance and no
 * thermal excursion or offline alert is opened for it: opening the door, unplugging the unit or
 * swapping a probe would otherwise raise false alarms. Coverage is decided by the gateway's own
 * timestamps, so a reading delivered late from the gateway's offline buffer is judged by when it was
 * taken, not by when it arrived.</p>
 *
 * <p><strong>Identity and idempotency.</strong> A window is identified on the platform by a
 * surrogate {@code id} and, toward the gateway, by {@code startEventUid}: the identifier of the
 * event that started it, unique across the platform, so a retried start never opens a second
 * window.</p>
 *
 * <p><strong>Lifecycle.</strong> A unit holds at most one active window, shared by the mobile app and
 * the hardware button. {@link #open} starts one that expires after the lifetime of its
 * {@link MaintenanceOrigin}. {@link #promoteToApp} lets a technician take over a window started with
 * the button: the window keeps its identity, becomes {@code APP}, gains the technician and the order
 * and gets a fresh expiration. {@link #close} ends it once; closing it again changes nothing.</p>
 *
 * <p>The domain events carry the surrogate identity, so the opening is announced through
 * {@link #onOpened} once the repository has assigned it, the same way {@code Alert#onRaised} works.</p>
 */
@Getter
public class MaintenanceWindow extends AbstractDomainAggregateRoot<MaintenanceWindow> {

  private Long id;
  private final UUID startEventUid;
  private final Long equipmentId;
  private MaintenanceOrigin origin;
  private final Instant startedAt;
  private Instant expiresAt;
  private Instant endedAt;
  private ClosureReason closureReason;
  private Long technicianProfileId;
  private Long serviceRequestId;

  /**
   * Reconstitutes a window, typically from persistence.
   *
   * @param id                  the persistence identity, or {@code null} when not yet persisted
   * @param startEventUid       identifier of the gateway event that started the window; required
   * @param equipmentId         the unit under maintenance; required
   * @param origin              how the window was started, or promoted to; required
   * @param startedAt           when the gateway started it; required
   * @param expiresAt           when it expires unless closed earlier; required, after {@code startedAt}
   * @param endedAt             when it ended, or {@code null} while active
   * @param closureReason       why it ended; required exactly when {@code endedAt} is set
   * @param technicianProfileId the technician attributed, or {@code null} when unknown
   * @param serviceRequestId    the order linked, or {@code null} when unlinked
   * @throws NullPointerException     when a required value is missing
   * @throws IllegalArgumentException when the instants or the closure are inconsistent
   */
  public MaintenanceWindow(Long id, UUID startEventUid, Long equipmentId, MaintenanceOrigin origin,
                           Instant startedAt, Instant expiresAt, Instant endedAt, ClosureReason closureReason,
                           Long technicianProfileId, Long serviceRequestId) {
    this.id = id;
    this.startEventUid = Objects.requireNonNull(startEventUid, "startEventUid must not be null");
    this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    this.origin = Objects.requireNonNull(origin, "origin must not be null");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    if (!expiresAt.isAfter(startedAt)) {
      throw new IllegalArgumentException("A maintenance window must expire after it starts");
    }
    if ((endedAt == null) != (closureReason == null)) {
      throw new IllegalArgumentException("A maintenance window has an end and a closure reason, or neither");
    }
    if (endedAt != null && endedAt.isBefore(startedAt)) {
      throw new IllegalArgumentException("A maintenance window cannot end before it starts");
    }
    this.endedAt = endedAt;
    this.closureReason = closureReason;
    this.technicianProfileId = technicianProfileId;
    this.serviceRequestId = serviceRequestId;
  }

  /**
   * Opens a new window, which expires after the lifetime of its origin.
   *
   * @param startEventUid       identifier of the gateway event that started it; required
   * @param equipmentId         the unit under maintenance; required
   * @param origin              how it was started; required
   * @param startedAt           when the gateway started it, by its clock; required
   * @param technicianProfileId the technician working on the unit, or {@code null} when unknown
   * @param serviceRequestId    the order the work belongs to, or {@code null} when unknown
   * @return the new active window, not yet persisted
   */
  public static MaintenanceWindow open(UUID startEventUid, Long equipmentId, MaintenanceOrigin origin,
                                       Instant startedAt, Long technicianProfileId, Long serviceRequestId) {
    Objects.requireNonNull(origin, "origin must not be null");
    return new MaintenanceWindow(null, startEventUid, equipmentId, origin, startedAt, origin.expiryFrom(startedAt),
        null, null, technicianProfileId, serviceRequestId);
  }

  public void assignId(Long id) {
    this.id = id;
  }

  /**
   * Announces that this window has just been opened and persisted.
   *
   * @throws IllegalStateException when the window has no identity yet
   */
  public void onOpened() {
    requirePersisted("opened");
    registerDomainEvent(MaintenanceWindowOpenedEvent.from(this));
  }

  /**
   * Hands a window started with the hardware button over to the mobile app.
   *
   * <p>The window keeps its identity and its start; its origin becomes {@code APP}, it is attributed
   * to the technician and the order the app command was authorized for, and its expiration is reset.
   * A window already {@code APP}, or already closed, is left untouched.</p>
   *
   * @param technicianId     the technician taking the window over, or {@code null} to keep the current one
   * @param serviceRequestId the order the technician works on, or {@code null} to keep the current one
   * @param newExpiry        the new expiration; required, after the start
   * @return {@code true} when the window was promoted
   * @throws IllegalArgumentException when the new expiration does not follow the start
   */
  public boolean promoteToApp(Long technicianId, Long serviceRequestId, Instant newExpiry) {
    Objects.requireNonNull(newExpiry, "newExpiry must not be null");
    if (origin != MaintenanceOrigin.BUTTON || endedAt != null) {
      return false;
    }
    if (!newExpiry.isAfter(startedAt)) {
      throw new IllegalArgumentException("A maintenance window must expire after it starts");
    }
    this.origin = MaintenanceOrigin.APP;
    if (technicianId != null) {
      this.technicianProfileId = technicianId;
    }
    if (serviceRequestId != null) {
      this.serviceRequestId = serviceRequestId;
    }
    // An APP window always expires one APP lifetime after it was renewed, so the renewal instant is
    // recovered from the expiration rather than passed alongside it.
    var promotedAt = newExpiry.minus(MaintenanceOrigin.APP.lifetime());
    this.expiresAt = newExpiry;
    if (id != null) {
      registerDomainEvent(MaintenanceWindowPromotedEvent.from(this, promotedAt));
    }
    return true;
  }

  /**
   * Attributes an active window to an order and its technician, found after it was opened.
   *
   * @param technicianId     the technician working on the order; required
   * @param serviceRequestId the order; required
   * @return {@code true} when the window was unlinked and is now linked
   */
  public boolean linkTo(Long technicianId, Long serviceRequestId) {
    Objects.requireNonNull(technicianId, "technicianId must not be null");
    Objects.requireNonNull(serviceRequestId, "serviceRequestId must not be null");
    if (!isUnlinked() || endedAt != null) {
      return false;
    }
    this.technicianProfileId = technicianId;
    this.serviceRequestId = serviceRequestId;
    return true;
  }

  /**
   * Ends the window. Idempotent: a window already closed keeps its first end and reason.
   *
   * <p>An end reported before the start, by a drifting gateway clock, is moved to the start so the
   * window never has a negative length.</p>
   *
   * @param endedAt when the window ended; required
   * @param reason  why it ended; required
   * @return {@code true} when this call closed the window
   * @throws IllegalStateException when the window has no identity yet
   */
  public boolean close(Instant endedAt, ClosureReason reason) {
    Objects.requireNonNull(endedAt, "endedAt must not be null");
    Objects.requireNonNull(reason, "reason must not be null");
    if (this.endedAt != null) {
      return false;
    }
    requirePersisted("closed");
    this.endedAt = endedAt.isBefore(startedAt) ? startedAt : endedAt;
    this.closureReason = reason;
    registerDomainEvent(MaintenanceWindowClosedEvent.from(this));
    return true;
  }

  /**
   * Tells whether an instant falls inside this window.
   *
   * @param instant the instant to test; {@code null} is never covered
   * @return {@code true} when the instant is at or after the start and, for a closed window, before
   *         the end
   */
  public boolean covers(Instant instant) {
    return instant != null && !instant.isBefore(startedAt) && (endedAt == null || instant.isBefore(endedAt));
  }

  /**
   * @return {@code true} when no order is linked to this window
   */
  public boolean isUnlinked() {
    return serviceRequestId == null;
  }

  /**
   * @return {@code true} while the window has not been closed
   */
  public boolean isActive() {
    return endedAt == null;
  }

  private void requirePersisted(String transition) {
    if (id == null) {
      throw new IllegalStateException("A maintenance window must be persisted before it is " + transition);
    }
  }
}
