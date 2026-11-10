package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;

/**
 * REST representation of how urgently an alert must be surfaced, as accepted by the alert listing
 * filter.
 *
 * <p>A separate type from the domain {@code AlertSeverity} so that an unrecognized value is rejected
 * at the boundary with a 400 naming the accepted ones. Values are matched exactly, in upper case.</p>
 */
@Schema(
    name = "AlertSeverity",
    description = "How urgently an alert should be surfaced",
    example = "CRITICAL")
public enum AlertSeverityResource {
  INFO,
  WARNING,
  CRITICAL;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain severity
   */
  public AlertSeverity toDomain() {
    return AlertSeverity.valueOf(name());
  }
}
