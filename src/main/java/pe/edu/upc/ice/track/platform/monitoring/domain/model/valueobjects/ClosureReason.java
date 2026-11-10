package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * Why a maintenance window ended, as reported by the gateway or decided by the platform.
 */
public enum ClosureReason {

  /** The technician ended it from the mobile app. */
  MANUAL_APP,

  /** Somebody held the hardware button of the gateway. */
  MANUAL_BUTTON,

  /** The intervention was registered, or the order's edge grant disappeared from the configuration. */
  INTERVENTION_COMPLETED,

  /** Its lifetime ran out without anybody ending it. */
  EXPIRED,

  /** The gateway ended it because the temperature reached a limit no maintenance may exceed. */
  SAFETY_LIMIT
}
