package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * How a maintenance window was started on the unit's gateway.
 *
 * <p>Each origin carries the lifetime of the window it starts: a window started from the mobile app
 * by an assigned technician lasts longer than one started by pressing the physical button, which
 * nobody is accountable for until it is linked to a service request.</p>
 */
public enum MaintenanceOrigin {

  /** Started from the mobile app, authorized by the technician's edge grant. Lasts two hours. */
  APP(Duration.ofHours(2)),

  /** Started by pressing the hardware button of the gateway. Lasts thirty minutes. */
  BUTTON(Duration.ofMinutes(30));

  private final Duration lifetime;

  MaintenanceOrigin(Duration lifetime) {
    this.lifetime = lifetime;
  }

  /**
   * @return how long a window of this origin stays open when nothing closes it earlier
   */
  public Duration lifetime() {
    return lifetime;
  }

  /**
   * Computes when a window of this origin, started or renewed at an instant, expires.
   *
   * @param from when the window was started or renewed; required
   * @return the expiration instant
   */
  public Instant expiryFrom(Instant from) {
    return Objects.requireNonNull(from, "from must not be null").plus(lifetime);
  }
}
