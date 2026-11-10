package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.DeviceTokenPersistenceEntity;

/** Bidirectional mapper between {@link DeviceToken} and {@link DeviceTokenPersistenceEntity}. */
public final class DeviceTokenPersistenceAssembler {

  private DeviceTokenPersistenceAssembler() {
  }

  public static DeviceTokenPersistenceEntity toPersistenceEntityFromDomain(DeviceToken deviceToken) {
    var entity = new DeviceTokenPersistenceEntity();
    entity.setId(deviceToken.getId());
    entity.setRecipientUserId(deviceToken.getRecipientUserId());
    entity.setToken(deviceToken.getToken());
    entity.setPlatform(deviceToken.getPlatform());
    entity.setLastSeenAt(deviceToken.getLastSeenAt());
    return entity;
  }

  public static DeviceToken toDomainFromPersistenceEntity(DeviceTokenPersistenceEntity entity) {
    return DeviceToken.rehydrate(
        entity.getId(), entity.getRecipientUserId(), entity.getToken(), entity.getPlatform(), entity.getLastSeenAt());
  }
}
