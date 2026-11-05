package pe.edu.upc.ice.track.platform.notifications.infrastructure.monitoring.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.AlertRaisedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyAlertRaisedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.AlertId;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

/**
 * Inbound infrastructure adapter that subscribes to {@link AlertRaisedIntegrationEvent}, the
 * published language of Monitoring and Alerting, and hands it to the notifications application
 * layer.
 *
 * <p>This is the only class in {@code notifications} that knows a {@code monitoring} context
 * exists, and the single Anti-Corruption Layer point where the {@code AlertType}/
 * {@code AlertSeverity} vocabulary of Monitoring - received as plain text - is mapped onto the
 * {@link NotificationType}/{@link NotificationSeverity} vocabulary of this context. The
 * application layer only ever sees the resulting {@link NotifyAlertRaisedCommand}. Notifications
 * never subscribes to {@code monitoring.domain.model.events.AlertRaisedEvent}, only to its
 * integration-event counterpart.</p>
 */
@Component
public class AlertRaisedEventListener {

  private final NotificationCommandService notificationCommandService;

  public AlertRaisedEventListener(NotificationCommandService notificationCommandService) {
    this.notificationCommandService = notificationCommandService;
  }

  /**
   * Translates the integration event into a {@link NotifyAlertRaisedCommand} and dispatches it.
   *
   * @param event the integration event published by Monitoring and Alerting
   * @throws IllegalArgumentException when the alert type or severity has no notification
   *                                  counterpart
   */
  @EventListener
  public void on(AlertRaisedIntegrationEvent event) {
    notificationCommandService.handle(new NotifyAlertRaisedCommand(
        new AlertId(event.alertId()),
        new EquipmentId(event.equipmentId()),
        toNotificationType(event.type()),
        toNotificationSeverity(event.severity()),
        event.peakTemperature()));
  }

  private static NotificationType toNotificationType(String alertType) {
    return switch (alertType) {
      case "TEMPERATURE_EXCURSION" -> NotificationType.TEMPERATURE_EXCURSION;
      case "DEVICE_OFFLINE" -> NotificationType.DEVICE_OFFLINE;
      case "SENSOR_FAILURE" -> NotificationType.SENSOR_FAILURE;
      default -> throw new IllegalArgumentException("Unmapped alert type: " + alertType);
    };
  }

  private static NotificationSeverity toNotificationSeverity(String alertSeverity) {
    return switch (alertSeverity) {
      case "INFO" -> NotificationSeverity.INFO;
      case "CRITICAL" -> NotificationSeverity.CRITICAL;
      case "WARNING" -> NotificationSeverity.WARNING;
      default -> throw new IllegalArgumentException("Unmapped alert severity: " + alertSeverity);
    };
  }
}
