package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

/**
 * REST representation of the kind of condition an alert was raised for, as accepted by the alert
 * listing filter.
 *
 * <p>A separate type from the domain {@code AlertType} so that an unrecognized value is rejected at
 * the boundary with a 400 naming the accepted ones. Values are matched exactly, in upper case.</p>
 */
@Schema(
    name = "AlertType",
    description = "Kind of anomalous condition an alert was raised for",
    example = "TEMPERATURE_EXCURSION")
public enum AlertTypeResource {
  TEMPERATURE_EXCURSION,
  DEVICE_OFFLINE;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain type
   */
  public AlertType toDomain() {
    return AlertType.valueOf(name());
  }
}
