package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.NotificationResource;

/** Maps a {@link Notification} into a {@link NotificationResource}. */
public final class NotificationResourceFromEntityAssembler {

  private NotificationResourceFromEntityAssembler() {
  }

  public static NotificationResource toResourceFromEntity(Notification notification) {
    return new NotificationResource(
        notification.getId(),
        notification.getEquipmentId().equipmentId(),
        notification.getDeviceId().deviceId(),
        notification.getSourceAlertId().alertId(),
        notification.getMessage(),
        notification.getType().name(),
        notification.getSeverity().name(),
        notification.isRead(),
        notification.getReadAt(),
        notification.getDismissedAt());
  }
}
