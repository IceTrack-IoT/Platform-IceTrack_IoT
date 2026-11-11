package pe.edu.upc.ice.track.platform.device.domain.model.valueobjects;

/**
 * Whether a board is reporting, as evaluated by the server when it is read.
 */
public enum DeviceConnectivityState {

  /** Paired, and its latest batch arrived within the offline window of its unit's policy. */
  REPORTING,

  /** Paired, but its latest batch arrived longer ago than the offline window. */
  SILENT,

  /** Paired, and no batch has arrived yet. */
  NEVER_REPORTED,

  /** Still linked to its unit, but its API key was withdrawn, so it cannot send anything. */
  KEY_REVOKED,

  /** Linked to no unit. */
  UNPAIRED
}
