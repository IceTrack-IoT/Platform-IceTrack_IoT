package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

/**
 * Closed set of notification kinds recognized by the {@code notifications} bounded context.
 *
 * <p>Enum-as-Value-Object, the same discipline applied to {@code AlertType} in
 * {@code monitoring}: a closed, discrete set needs no further validation beyond membership.</p>
 */
public enum NotificationType {
  MAINTENANCE_REMINDER,
  TEMPERATURE_EXCURSION,
  DEVICE_OFFLINE,
  LOW_BATTERY,
  SENSOR_FAILURE,
  SERVICE_REQUEST_UPDATE
}
