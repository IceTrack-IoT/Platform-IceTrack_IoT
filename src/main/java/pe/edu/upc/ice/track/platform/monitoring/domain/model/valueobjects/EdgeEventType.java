package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * Kind of event an edge gateway reports on {@code POST /api/v1/telemetry/events}.
 */
public enum EdgeEventType {

  /**
   * The temperature stayed outside the configured range for the policy's sustained duration. Opens
   * a CRITICAL thermal excursion alert unless one is already active, or a maintenance window covers
   * the instant it was detected at.
   */
  THERMAL_EXCURSION_DETECTED,

  /**
   * The temperature came back within range, by the hysteresis margin, for the sustained duration.
   * Resolves the active thermal excursion alert.
   */
  THERMAL_EXCURSION_NORMALIZED,

  /**
   * A maintenance window started on the gateway, from the mobile app or the hardware button. Opens
   * the unit's window, or takes over the active one when the app starts maintenance on a window the
   * button started.
   */
  MAINTENANCE_WINDOW_STARTED,

  /**
   * The gateway ended the unit's maintenance window, with a closure reason.
   */
  MAINTENANCE_WINDOW_ENDED
}
