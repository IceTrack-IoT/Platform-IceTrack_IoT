package pe.edu.upc.ice.track.platform.notifications.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.AlertRaisedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.assetmanagement.ExternalAssetManagementServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.AlertId;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;

import java.util.Optional;

/**
 * Subscribes to {@link AlertRaisedIntegrationEvent}, the published language of Monitoring and
 * Alerting Management, and translates it into a {@code Notification} for the equipment's owner.
 *
 * <p>This is the single Anti-Corruption Layer point where the {@code AlertType}/
 * {@code AlertSeverity} vocabulary of Monitoring is mapped onto the
 * {@code NotificationType}/{@code NotificationSeverity} vocabulary of this context -- by
 * design, no other class performs this mapping. Notifications never subscribes to
 * {@code monitoring.domain.model.events.AlertRaisedEvent}, only to its integration-event
 * counterpart, honoring the same boundary discipline every other cross-context subscription in
 * this platform follows.</p>
 */
@Service("notificationsAlertRaisedEventHandler")
public class AlertRaisedEventHandler {

  private final NotificationCommandService notificationCommandService;
  private final ExternalAssetManagementServiceForNotifications externalAssetManagementService;

  public AlertRaisedEventHandler(
      NotificationCommandService notificationCommandService,
      ExternalAssetManagementServiceForNotifications externalAssetManagementService) {
    this.notificationCommandService = notificationCommandService;
    this.externalAssetManagementService = externalAssetManagementService;
  }

  @EventListener
  public void on(AlertRaisedIntegrationEvent event) {
    Optional<Double> recipientUserId = externalAssetManagementService.fetchEquipmentOwnerId(event.equipmentId());
    if (recipientUserId ==  null || recipientUserId.isEmpty()) {
      new RuntimeException("No recipient user ID found for equipment ID: " + event.equipmentId());
      return;
    }

    var command = new CreateNotificationCommand(
        recipientUserId.get().longValue(),
        new EquipmentId(event.equipmentId()),
        null,
        new AlertId(event.alertId()),
        buildMessage(event),
        mapAlertTypeToNotificationType(event.type()),
        mapAlertSeverityToNotificationSeverity(event.severity()));

    notificationCommandService.handle(command);
  }

  private NotificationType mapAlertTypeToNotificationType(String alertType) {
    return switch (alertType) {
      case "DEVICE_OFFLINE" -> NotificationType.DEVICE_OFFLINE;
      case "SENSOR_FAILURE" -> NotificationType.SENSOR_FAILURE;
      default -> NotificationType.TEMPERATURE_EXCURSION;
    };
  }

  private NotificationSeverity mapAlertSeverityToNotificationSeverity(String alertSeverity) {
    return switch (alertSeverity) {
      case "CRITICAL" -> NotificationSeverity.CRITICAL;
      case "WARNING" -> NotificationSeverity.WARNING;
      default -> NotificationSeverity.INFO;
    };
  }

  private String buildMessage(AlertRaisedIntegrationEvent event) {
    if (event.peakTemperature() == null) {
      return "Equipment %d reported a new %s alert.".formatted(event.equipmentId(), event.type());
    }
    return "Equipment %d reported a %s alert, peak temperature %.1f C."
        .formatted(event.equipmentId(), event.type(), event.peakTemperature());
  }
}
