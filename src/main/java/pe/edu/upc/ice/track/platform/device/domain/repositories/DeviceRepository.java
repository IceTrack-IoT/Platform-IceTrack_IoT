package pe.edu.upc.ice.track.platform.device.domain.repositories;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.util.List;
import java.util.Optional;

/** Persistence port for the Device Management aggregate. */
public interface DeviceRepository {
  Optional<Device> findById(Long deviceId);

  Optional<Device> findByDeviceUid(String deviceUid);

  List<Device> findByEquipmentId(Long equipmentId);

  boolean existsByDeviceUid(String deviceUid);

  Device save(Device device);
}
