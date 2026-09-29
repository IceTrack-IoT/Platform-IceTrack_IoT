package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.SensorReadingPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.SensorReadingPersistenceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter fulfilling {@link SensorReadingRepository} on top of Spring Data JPA. */
@Repository
public class SensorReadingRepositoryImpl implements SensorReadingRepository {

  private final SensorReadingPersistenceRepository persistenceRepository;

  public SensorReadingRepositoryImpl(SensorReadingPersistenceRepository persistenceRepository) {
    this.persistenceRepository = persistenceRepository;
  }

  @Override
  public SensorReading save(SensorReading reading) {
    var entity = SensorReadingPersistenceAssembler.toPersistenceEntityFromDomain(reading);
    var saved = persistenceRepository.save(entity);
    return SensorReadingPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<SensorReading> findByReadingUid(UUID readingUid) {
    return persistenceRepository.findByReadingUid(readingUid)
        .map(SensorReadingPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public List<SensorReading> findByEquipmentIdAndRecordedAtBetween(
      Long equipmentId, LocalDateTime from, LocalDateTime to) {
    return persistenceRepository.findByEquipmentIdAndRecordedAtBetween(equipmentId, from, to)
        .stream()
        .map(SensorReadingPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  public List<SensorReading> findTopByEquipmentIdOrderByRecordedAtDesc(Long equipmentId, int limit) {
    return persistenceRepository.findTop50ByEquipmentIdOrderByRecordedAtDesc(equipmentId)
        .stream()
        .limit(limit)
        .map(SensorReadingPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }
}
