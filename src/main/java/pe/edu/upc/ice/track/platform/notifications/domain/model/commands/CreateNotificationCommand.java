package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

/**
 * Command to create a new notification for a recipient.
 *
 * @param recipientUserId  identifier of the user the notification is addressed to; required
 * @param equipmentId      equipment the notification concerns, may be {@code null}
 * @param deviceId         device the notification concerns, may be {@code null}
 * @param sourceAlertId    identifier of the originating Alert in Monitoring and Alerting
 *                         Management, may be {@code null} when the notification has no alert
 *                         as its source
 * @param message          final, already-resolved English text shown to the recipient; required
 * @param type             kind of notification; required
 * @param severity         urgency of the notification; required
 * @param serviceRequestId identifier of the service request the notification is about, in Service
 *                         Request Management; {@code null} when it concerns none
 * @param template         the template key and values clients localize the text from; {@code null}
 *                         is stored as {@link MessageTemplate#untemplated()}
 */
public record CreateNotificationCommand(
    Long recipientUserId,
    EquipmentId equipmentId,
    DeviceId deviceId,
    AlertId sourceAlertId,
    String message,
    NotificationType type,
    NotificationSeverity severity,
    Long serviceRequestId,
    MessageTemplate template) {

  /**
   * Creates the command without a template.
   */
  public CreateNotificationCommand(Long recipientUserId, EquipmentId equipmentId, DeviceId deviceId,
                                   AlertId sourceAlertId, String message, NotificationType type,
                                   NotificationSeverity severity, Long serviceRequestId) {
    this(recipientUserId, equipmentId, deviceId, sourceAlertId, message, type, severity, serviceRequestId, null);
  }
}
