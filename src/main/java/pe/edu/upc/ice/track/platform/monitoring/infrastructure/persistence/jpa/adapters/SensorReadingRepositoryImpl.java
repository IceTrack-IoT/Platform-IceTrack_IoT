package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.SensorReadingPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.SensorReadingPersistenceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter fulfilling {@link SensorReadingRepository} on top of Spring Data JPA.
 *
 * <p>Publishes the domain events a reading registered, such as {@code ReadingRecordedEvent}, once it
 * is saved, in the caller's transaction, exactly like {@code AlertRepositoryImpl}.</p>
 */
@Repository
public class SensorReadingRepositoryImpl implements SensorReadingRepository {

  private final SensorReadingPersistenceRepository persistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public SensorReadingRepositoryImpl(SensorReadingPersistenceRepository persistenceRepository,
                                     ApplicationEventPublisher eventPublisher) {
    this.persistenceRepository = persistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public SensorReading save(SensorReading reading) {
    var entity = SensorReadingPersistenceAssembler.toPersistenceEntityFromDomain(reading);
    var saved = persistenceRepository.save(entity);
    var savedReading = SensorReadingPersistenceAssembler.toDomainFromPersistenceEntity(saved);
    reading.domainEvents().forEach(eventPublisher::publishEvent);
    reading.clearDomainEvents();
    return savedReading;
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
