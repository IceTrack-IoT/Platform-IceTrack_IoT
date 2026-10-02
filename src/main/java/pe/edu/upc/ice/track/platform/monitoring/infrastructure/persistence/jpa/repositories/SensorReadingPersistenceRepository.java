package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.SensorReadingPersistenceEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository operating on {@link SensorReadingPersistenceEntity}. */
public interface SensorReadingPersistenceRepository
    extends JpaRepository<SensorReadingPersistenceEntity, Long> {

  Optional<SensorReadingPersistenceEntity> findByReadingUid(UUID readingUid);

  List<SensorReadingPersistenceEntity> findByEquipmentIdAndRecordedAtBetween(
      Long equipmentId, LocalDateTime from, LocalDateTime to);

  List<SensorReadingPersistenceEntity> findTop50ByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);
}
