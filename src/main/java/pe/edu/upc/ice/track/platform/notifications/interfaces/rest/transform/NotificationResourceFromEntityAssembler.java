package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.NotificationResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.PagedNotificationResource;

/** Maps a {@link Notification}, or a page of them, into its REST representation. */
public final class NotificationResourceFromEntityAssembler {

  private NotificationResourceFromEntityAssembler() {
  }

  public static NotificationResource toResourceFromEntity(Notification notification) {
    return new NotificationResource(
        notification.getId(),
        notification.getRecipientUserId(),
        notification.getEquipmentId() == null ? null : notification.getEquipmentId().equipmentId(),
        notification.getDeviceId() == null ? null : notification.getDeviceId().deviceId(),
        notification.getSourceAlertId() == null ? null : notification.getSourceAlertId().alertId(),
        notification.getServiceRequestId(),
        notification.getMessage(),
        notification.getTemplate().key(),
        notification.getTemplate().params(),
        notification.getType().name(),
        notification.getSeverity().name(),
        notification.isRead(),
        notification.getCreatedAt(),
        notification.getReadAt(),
        notification.getDismissedAt());
  }

  /**
   * Converts a {@link NotificationPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedNotificationResource} resource
   */
  public static PagedNotificationResource toPagedResourceFromPage(NotificationPage page) {
    return new PagedNotificationResource(
        page.content().stream().map(NotificationResourceFromEntityAssembler::toResourceFromEntity).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
