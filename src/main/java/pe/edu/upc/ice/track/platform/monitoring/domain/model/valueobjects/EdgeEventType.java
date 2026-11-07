package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * Kind of event an edge gateway reports on {@code POST /api/v1/telemetry/events}.
 */
public enum EdgeEventType {

  /**
   * The temperature stayed outside the configured range for the policy's sustained duration. Opens
   * a CRITICAL thermal excursion alert unless one is already active.
   */
  THERMAL_EXCURSION_DETECTED,

  /**
   * The temperature came back within range, by the hysteresis margin, for the sustained duration.
   * Resolves the active thermal excursion alert.
   */
  THERMAL_EXCURSION_NORMALIZED
}
