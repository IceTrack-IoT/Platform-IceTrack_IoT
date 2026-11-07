package pe.edu.upc.ice.track.platform.device.domain.repositories;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.util.List;
import java.util.Optional;

/** Persistence port for the Device Management aggregate. */
public interface DeviceRepository {
  Optional<Device> findById(Long deviceId);

  Optional<Device> findByDeviceUid(String deviceUid);

  /** Finds the board holding the given hex SHA-256 API key digest, through a unique index. */
  Optional<Device> findByApiKeyDigest(String apiKeyDigest);

  List<Device> findByEquipmentId(Long equipmentId);

  /** Tells whether any board, paired or revoked, is currently linked to the equipment. */
  boolean existsLinkedToEquipment(Long equipmentId);

  Device save(Device device);
}
