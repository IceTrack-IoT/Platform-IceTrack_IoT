package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for the Device Management persistence entity. */
@Repository
public interface DevicePersistenceRepository extends JpaRepository<DevicePersistenceEntity, Long> {
  Optional<DevicePersistenceEntity> findByDeviceUid(String deviceUid);

  List<DevicePersistenceEntity> findByPairedEquipmentIdOrderByIdAsc(Long equipmentId);

  boolean existsByDeviceUid(String deviceUid);
}
