package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the Device Management persistence entity.
 *
 * <p>The owner inventory is served through {@link JpaSpecificationExecutor} with the specification
 * built by {@code DevicePersistenceSpecifications}: its filters are optional and combine freely,
 * which a derived query name cannot express.</p>
 */
@Repository
public interface DevicePersistenceRepository
    extends JpaRepository<DevicePersistenceEntity, Long>, JpaSpecificationExecutor<DevicePersistenceEntity> {
  Optional<DevicePersistenceEntity> findByDeviceUid(String deviceUid);

  Optional<DevicePersistenceEntity> findByApiKeyDigest(String apiKeyDigest);

  List<DevicePersistenceEntity> findByPairedEquipmentIdOrderByIdAsc(Long equipmentId);

  boolean existsByPairedEquipmentId(Long equipmentId);

  /**
   * Counts the boards in a status that are linked to one of the given units.
   *
   * @param equipmentIds identifiers of the units
   * @param status       the status to count
   * @return the number of matching rows
   */
  long countByPairedEquipmentIdInAndStatus(Collection<Long> equipmentIds, DeviceStatus status);
}
