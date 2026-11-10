package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * Whether a refrigeration unit is reporting, as judged by the server.
 */
public enum ConnectivityState {

  /** A reading batch arrived within the unit's offline window. */
  REPORTING,

  /** The latest reading batch arrived longer ago than the unit's offline window. */
  SILENT,

  /** No reading batch was ever stored for the unit. */
  NEVER_REPORTED,

  /** The unit is {@code DEACTIVATED}, so its silence is expected and not judged. */
  NOT_MONITORED
}
