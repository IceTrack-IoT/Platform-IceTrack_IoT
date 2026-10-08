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

  /**
   *  Finds a sensor reading by its unique identifier.
   * @param readingUid  unique identifier of the sensor reading
   * @return  an Optional containing the sensor reading if found, or empty if not found
   */
  Optional<SensorReadingPersistenceEntity> findByReadingUid(UUID readingUid);

  /**
   *  Finds sensor readings for a specific equipment within a given time range.
   * @param equipmentId identifier of the equipment
   * @param from  inclusive start of the time range
   * @param to  inclusive end of the time range
   * @return  a list of sensor readings matching the criteria
   */
  List<SensorReadingPersistenceEntity> findByEquipmentIdAndRecordedAtBetween(
      Long equipmentId, LocalDateTime from, LocalDateTime to);

  /**
   *  Finds the most recent 50 sensor readings for a specific equipment, ordered by recorded time in descending order.
   * @param equipmentId identifier of the equipment
   * @return  a list of the most recent 50 sensor readings for the specified equipment
   */
  List<SensorReadingPersistenceEntity> findTop50ByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);
}
