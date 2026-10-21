package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;

/** Maps the Device aggregate to and from its JPA-only representation. */
public final class DevicePersistenceAssembler {

  private DevicePersistenceAssembler() {
  }

  public static Device toDomainFromPersistence(DevicePersistenceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Device(
        entity.getId(),
        entity.getDeviceUid(),
        entity.getBoardModel(),
        entity.getFirmwareVersion(),
        entity.getApiKeyHash(),
        entity.getStatus(),
        entity.getPairedEquipmentId(),
        entity.getLastSeenAt());
  }

  public static DevicePersistenceEntity toPersistenceFromDomain(Device device) {
    if (device == null) {
      return null;
    }
    var entity = new DevicePersistenceEntity();
    entity.setId(device.getDeviceId());
    entity.setDeviceUid(device.getDeviceUid());
    entity.setBoardModel(device.getBoardModel());
    entity.setFirmwareVersion(device.getFirmwareVersion());
    entity.setApiKeyHash(device.getApiKeyHash());
    entity.setStatus(device.getStatus());
    entity.setPairedEquipmentId(device.getPairedEquipmentId());
    entity.setLastSeenAt(device.getLastSeenAt());
    return entity;
  }
}
