package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;

/**
 * Bidirectional mapper between {@link Notification} and {@link NotificationPersistenceEntity}.
 *
 * <p>Unlike {@code AlertPersistenceAssembler} in Monitoring and Alerting Management, this
 * assembler restores every field on rehydration -- including {@code isRead}, {@code readAt}
 * and {@code dismissedAt} -- via {@link Notification#rehydrate}, so there is no documented
 * rehydration gap here.</p>
 */
public final class NotificationPersistenceAssembler {

  private NotificationPersistenceAssembler() {
  }

  /**
   *  Maps a {@link Notification} domain object to a {@link NotificationPersistenceEntity} for persistence.
   * @param notification  the domain object to be mapped; must not be null
   * @return  the corresponding persistence entity; never null
   */
  public static NotificationPersistenceEntity toPersistenceEntityFromDomain(Notification notification) {
    var entity = new NotificationPersistenceEntity();
    entity.setId(notification.getId());
    entity.setRecipientUserId(notification.getRecipientUserId());
    entity.setEquipmentId(notification.getEquipmentId().equipmentId());
    entity.setDeviceId(notification.getDeviceId().deviceId());
    entity.setSourceAlertId(notification.getSourceAlertId().alertId());
    entity.setMessage(notification.getMessage());
    entity.setType(NotificationType.valueOf(notification.getType().name()));
    entity.setSeverity(NotificationSeverity.valueOf(notification.getSeverity().name()));
    entity.setRead(notification.isRead());
    entity.setReadAt(notification.getReadAt());
    entity.setDismissedAt(notification.getDismissedAt());
    return entity;
  }

  /**
   *  Maps a {@link NotificationPersistenceEntity} to a {@link Notification} domain object for rehydration.
   * @param entity  the persistence entity to be mapped; must not be null
   * @return the corresponding domain object; never null
   */
  public static Notification toDomainFromPersistenceEntity(NotificationPersistenceEntity entity) {
    return Notification.rehydrate(
        entity.getId(),
        entity.getRecipientUserId(),
        new EquipmentId(entity.getEquipmentId()),
        new DeviceId(entity.getDeviceId()),
        new AlertId(entity.getSourceAlertId()),
        entity.getMessage(),
        NotificationType.valueOf(String.valueOf(entity.getType())),
        NotificationSeverity.valueOf(String.valueOf(entity.getSeverity())),
        entity.isRead(),
        entity.getReadAt(),
        entity.getDismissedAt());
  }
}
