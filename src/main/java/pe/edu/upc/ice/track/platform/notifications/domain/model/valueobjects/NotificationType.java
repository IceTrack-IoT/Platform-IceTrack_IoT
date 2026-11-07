package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

/**
 * Closed set of notification kinds recognized by the {@code notifications} bounded context.
 *
 * <p>Enum-as-Value-Object, the same discipline applied to {@code AlertType} in
 * {@code monitoring}: a closed, discrete set needs no further validation beyond membership. Every
 * name fits the 30 characters of the {@code notifications.type} column.</p>
 */
public enum NotificationType {
  MAINTENANCE_REMINDER,
  TEMPERATURE_EXCURSION,
  DEVICE_OFFLINE,
  LOW_BATTERY,
  SENSOR_FAILURE,
  SERVICE_REQUEST_UPDATE,

  /** A maintenance window opened or closed on one of the recipient's units. */
  MAINTENANCE_WINDOW,

  /** A maintenance window opened on one of the recipient's units with no service request to attribute it to. */
  UNLINKED_MAINTENANCE_WINDOW
}
