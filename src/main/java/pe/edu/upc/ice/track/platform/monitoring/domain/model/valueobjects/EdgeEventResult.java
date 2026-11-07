package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * What processing an edge event did to the unit's alerts.
 */
public enum EdgeEventResult {

  /** An excursion was detected and a new alert was opened. */
  ALERT_OPENED,

  /** An excursion was detected while an alert was already OPEN or ACKNOWLEDGED; nothing was opened. */
  ALERT_ALREADY_ACTIVE,

  /** An excursion normalized and the active alert was resolved. */
  ALERT_RESOLVED,

  /** An excursion normalized while no alert was active; nothing changed. */
  NO_ACTIVE_ALERT
}
