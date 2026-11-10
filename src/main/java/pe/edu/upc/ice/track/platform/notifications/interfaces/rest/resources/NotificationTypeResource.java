package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;

/**
 * REST representation of the kind of a notification, accepted by the inbox filters.
 */
@Schema(
    name = "NotificationType",
    description = "Kind of notification",
    example = "TEMPERATURE_EXCURSION")
public enum NotificationTypeResource {

  MAINTENANCE_REMINDER,
  TEMPERATURE_EXCURSION,
  DEVICE_OFFLINE,
  LOW_BATTERY,
  SENSOR_FAILURE,
  SERVICE_REQUEST_UPDATE,
  MAINTENANCE_WINDOW,
  UNLINKED_MAINTENANCE_WINDOW;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain type
   */
  public NotificationType toDomain() {
    return NotificationType.valueOf(name());
  }
}
