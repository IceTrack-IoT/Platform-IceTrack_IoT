package pe.edu.upc.ice.track.platform.notifications.test.fixtures;

import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Duration;
import java.util.Set;

import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

public final class NotificationsCommandFixtures {

  private NotificationsCommandFixtures() {
    throw new AssertionError("NotificationsCommandFixtures is a utility class and must not be instantiated.");
  }

  public static CreateNotificationCommand createNotificationCommand() {
    return new CreateNotificationCommand(RECIPIENT_ID, new EquipmentId(EQUIPMENT_ID), new DeviceId(DEVICE_ID),
        new AlertId(ALERT_ID), MESSAGE, NotificationType.TEMPERATURE_EXCURSION, NotificationSeverity.CRITICAL,
        SERVICE_REQUEST_ID);
  }

  public static MarkNotificationAsReadCommand markAsReadCommand() {
    return new MarkNotificationAsReadCommand(NOTIFICATION_ID, RECIPIENT_ID);
  }

  public static DismissNotificationCommand dismissCommand() {
    return new DismissNotificationCommand(NOTIFICATION_ID, RECIPIENT_ID);
  }

  public static MarkAllNotificationsAsReadCommand markAllAsReadCommand() {
    return new MarkAllNotificationsAsReadCommand(RECIPIENT_ID, NotificationFilter.NONE);
  }

  public static MarkNotificationsAsReadCommand markSomeAsReadCommand() {
    return new MarkNotificationsAsReadCommand(RECIPIENT_ID, Set.of(NOTIFICATION_ID, 13L));
  }

  public static NotifyAlertRaisedCommand excursionAlertCommand(Double peakTemperature) {
    return new NotifyAlertRaisedCommand(new AlertId(ALERT_ID), new EquipmentId(EQUIPMENT_ID),
        NotificationType.TEMPERATURE_EXCURSION, NotificationSeverity.CRITICAL, peakTemperature);
  }

  public static NotifyAlertRaisedCommand offlineAlertCommand(Long silentMinutes) {
    return new NotifyAlertRaisedCommand(new AlertId(ALERT_ID), new EquipmentId(EQUIPMENT_ID),
        NotificationType.DEVICE_OFFLINE, NotificationSeverity.WARNING, null, silentMinutes);
  }

  public static NotifyAlertRaisedCommand sensorFailureAlertCommand() {
    return new NotifyAlertRaisedCommand(new AlertId(ALERT_ID), new EquipmentId(EQUIPMENT_ID),
        NotificationType.SENSOR_FAILURE, NotificationSeverity.WARNING, null);
  }

  public static NotifyTechnicianAssignedCommand technicianAssignedCommand(Long previousTechnicianId) {
    return new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, previousTechnicianId, EQUIPMENT_ID,
        SITE_ID, "REPAIR", "HIGH", T0);
  }

  public static NotifyTechnicianAssignedCommand technicianReassignedCommand() {
    return technicianAssignedCommand(PREVIOUS_TECHNICIAN_ID);
  }

  public static NotifyServiceRequestRejectedCommand serviceRequestRejectedCommand() {
    return new NotifyServiceRequestRejectedCommand(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID,
        "Not enough spare parts");
  }

  public static NotifyServiceRequestCanceledCommand serviceRequestCanceledCommand() {
    return new NotifyServiceRequestCanceledCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, "Owner canceled");
  }

  public static NotifyMaintenanceWindowOpenedCommand windowOpenedCommand(String origin, Long serviceRequestId) {
    return new NotifyMaintenanceWindowOpenedCommand(WINDOW_ID, EQUIPMENT_ID, origin, T0, T0.plus(Duration.ofHours(2)),
        serviceRequestId == null ? null : TECHNICIAN_ID, serviceRequestId);
  }

  public static NotifyMaintenanceWindowClosedCommand windowClosedCommand(String origin, Long serviceRequestId) {
    return new NotifyMaintenanceWindowClosedCommand(WINDOW_ID, EQUIPMENT_ID, origin, T0.plusSeconds(600),
        "MANUAL_APP", serviceRequestId == null ? null : TECHNICIAN_ID, serviceRequestId);
  }

  public static RegisterDeviceTokenCommand registerTokenCommand() {
    return new RegisterDeviceTokenCommand(RECIPIENT_ID, TOKEN, DevicePlatform.ANDROID);
  }

  public static UnregisterDeviceTokenCommand unregisterTokenCommand() {
    return new UnregisterDeviceTokenCommand(RECIPIENT_ID, TOKEN);
  }
}
