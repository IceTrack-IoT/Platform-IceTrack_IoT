package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.ConnectivityState;

/**
 * REST representation of whether a unit is reporting, as judged by the server.
 */
@Schema(
    name = "ConnectivityState",
    description = "REPORTING: a batch arrived within the offline window. SILENT: the last batch arrived longer ago. "
        + "NEVER_REPORTED: no batch stored yet. NOT_MONITORED: the unit is DEACTIVATED",
    example = "REPORTING")
public enum ConnectivityStateResource {

  REPORTING,
  SILENT,
  NEVER_REPORTED,
  NOT_MONITORED;

  /**
   * Translates a domain state into this REST constant.
   *
   * @param state the domain state; required
   * @return the matching REST constant
   */
  public static ConnectivityStateResource fromDomain(ConnectivityState state) {
    return valueOf(state.name());
  }
}
