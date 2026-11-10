package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;

/**
 * REST representation of how urgently a notification should be surfaced, accepted by the inbox
 * filters.
 */
@Schema(
    name = "NotificationSeverity",
    description = "How urgently a notification should be surfaced",
    example = "CRITICAL")
public enum NotificationSeverityResource {

  INFO,
  WARNING,
  CRITICAL;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain severity
   */
  public NotificationSeverity toDomain() {
    return NotificationSeverity.valueOf(name());
  }
}
