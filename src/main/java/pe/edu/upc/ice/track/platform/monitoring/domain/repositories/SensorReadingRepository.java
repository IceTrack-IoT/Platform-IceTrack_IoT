package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository port for {@link SensorReading}.
 *
 * <p>Implemented by {@code SensorReadingRepositoryImpl} in the infrastructure layer, which
 * delegates to the Spring Data JPA repository and the persistence assembler.</p>
 */
public interface SensorReadingRepository {

  SensorReading save(SensorReading reading);

  Optional<SensorReading> findByReadingUid(UUID readingUid);

  List<SensorReading> findByEquipmentIdAndRecordedAtBetween(
      Long equipmentId, LocalDateTime from, LocalDateTime to);

  List<SensorReading> findTopByEquipmentIdOrderByRecordedAtDesc(Long equipmentId, int limit);
}
