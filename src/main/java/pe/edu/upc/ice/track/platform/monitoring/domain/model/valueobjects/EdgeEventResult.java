package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * What processing an edge event did to the unit's alerts or maintenance window.
 *
 * <p>Every name fits the 30 characters of the {@code edge_events.result} column.</p>
 */
public enum EdgeEventResult {

  /** An excursion was detected and a new alert was opened. */
  ALERT_OPENED,

  /** An excursion was detected while an alert was already OPEN or ACKNOWLEDGED; nothing was opened. */
  ALERT_ALREADY_ACTIVE,

  /** An excursion normalized and the active alert was resolved. */
  ALERT_RESOLVED,

  /** An excursion normalized while no alert was active; nothing changed. */
  NO_ACTIVE_ALERT,

  /** An excursion was detected at an instant a maintenance window covers; no alert was opened. */
  SUPPRESSED_BY_MAINTENANCE,

  /** A maintenance window started and was opened. */
  MAINTENANCE_WINDOW_OPENED,

  /** The app started maintenance on a window the button had started; the window became APP. */
  MAINTENANCE_WINDOW_PROMOTED,

  /** A maintenance window started while the unit already had one; the active window was kept. */
  MAINTENANCE_ALREADY_ACTIVE,

  /** The unit's maintenance window was closed. */
  MAINTENANCE_WINDOW_CLOSED,

  /** A maintenance window ended while none was active, or it was already closed; nothing changed. */
  NO_ACTIVE_MAINTENANCE
}
