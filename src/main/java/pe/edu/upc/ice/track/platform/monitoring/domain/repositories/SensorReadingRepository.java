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

  /**
   *  Persists a new sensor reading in the database.
   * @param reading the sensor reading to be persisted
   * @return  the persisted sensor reading with its generated identifier
   */
  SensorReading save(SensorReading reading);

  /**
   *  Fetches a sensor reading by its unique identifier.
   * @param readingUid  the unique identifier of the sensor reading
   * @return  an optional containing the sensor reading if found, or empty if not found
   */
  Optional<SensorReading> findByReadingUid(UUID readingUid);

  /**
   *  Fetches all sensor readings for a given equipment within a specified date range.
   * @param equipmentId the identifier of the equipment
   * @param from  the start of the date range (inclusive)
   * @param to  the end of the date range (inclusive)
   * @return  a list of sensor readings matching the criteria
   */
  List<SensorReading> findByEquipmentIdAndRecordedAtBetween(
      Long equipmentId, LocalDateTime from, LocalDateTime to);

  /**
   *  Fetches the most recent sensor readings for a given equipment, limited by the specified number of readings.
   * @param equipmentId the identifier of the equipment
   * @param limit the maximum number of readings to retrieve
   * @return a list of the most recent sensor readings for the equipment, ordered by recordedAt descending
   */
  List<SensorReading> findTopByEquipmentIdOrderByRecordedAtDesc(Long equipmentId, int limit);
}
