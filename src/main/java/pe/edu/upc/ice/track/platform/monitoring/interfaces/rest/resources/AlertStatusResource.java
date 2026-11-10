package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;

/**
 * REST representation of the lifecycle status of an alert, as accepted by the alert listing filter.
 *
 * <p>A separate type from the domain {@code AlertStatus} so that an unrecognized value is rejected at
 * the boundary with a 400 naming the accepted ones. Values are matched exactly, in upper case.</p>
 */
@Schema(
    name = "AlertStatus",
    description = "Lifecycle status of an alert: OPEN, then optionally ACKNOWLEDGED, and finally RESOLVED or DISMISSED",
    example = "OPEN")
public enum AlertStatusResource {
  OPEN,
  ACKNOWLEDGED,
  RESOLVED,
  DISMISSED;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain status
   */
  public AlertStatus toDomain() {
    return AlertStatus.valueOf(name());
  }
}
