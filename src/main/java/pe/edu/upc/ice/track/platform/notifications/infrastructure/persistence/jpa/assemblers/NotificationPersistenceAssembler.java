package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

/**
 * Bidirectional mapper between {@link Notification} and {@link NotificationPersistenceEntity}.
 *
 * <p>Unlike {@code AlertPersistenceAssembler} in Monitoring and Alerting Management, this
 * assembler restores every field on rehydration -- including {@code createdAt}, {@code isRead},
 * {@code readAt} and {@code dismissedAt} -- via {@link Notification#rehydrate}, so there is no
 * documented rehydration gap here.</p>
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
    entity.setEquipmentId(notification.getEquipmentId() == null ? null : notification.getEquipmentId().equipmentId());
    entity.setDeviceId(notification.getDeviceId() == null ? null : notification.getDeviceId().deviceId());
    entity.setSourceAlertId(notification.getSourceAlertId() == null ? null : notification.getSourceAlertId().alertId());
    entity.setServiceRequestId(notification.getServiceRequestId());
    entity.setMessage(notification.getMessage());
    entity.setMessageKey(notification.getTemplate().key());
    entity.setMessageParams(notification.getTemplate().params());
    entity.setType(NotificationType.valueOf(notification.getType().name()));
    entity.setSeverity(NotificationSeverity.valueOf(notification.getSeverity().name()));
    entity.setRead(notification.isRead());
    // Only used on insert: the column is not updatable, see NotificationPersistenceEntity.
    entity.setCreatedAt(notification.getCreatedAt());
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
        entity.getEquipmentId() == null ? null : new EquipmentId(entity.getEquipmentId()),
        entity.getDeviceId() == null ? null : new DeviceId(entity.getDeviceId()),
        entity.getSourceAlertId() == null ? null : new AlertId(entity.getSourceAlertId()),
        entity.getServiceRequestId(),
        entity.getMessage(),
        entity.getMessageKey() == null
            ? MessageTemplate.untemplated()
            : new MessageTemplate(entity.getMessageKey(), entity.getMessageParams()),
        entity.getType(),
        entity.getSeverity(),
        entity.isRead(),
        entity.getCreatedAt(),
        entity.getReadAt(),
        entity.getDismissedAt());
  }
}
