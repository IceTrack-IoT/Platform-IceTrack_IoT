package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.EdgeEventRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.EdgeEventPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.EdgeEventPersistenceRepository;

import java.util.Optional;
import java.util.UUID;

/** Adapter fulfilling {@link EdgeEventRepository} on top of Spring Data JPA. */
@Repository
public class EdgeEventRepositoryImpl implements EdgeEventRepository {

  private final EdgeEventPersistenceRepository persistenceRepository;

  public EdgeEventRepositoryImpl(EdgeEventPersistenceRepository persistenceRepository) {
    this.persistenceRepository = persistenceRepository;
  }

  @Override
  public EdgeEvent save(EdgeEvent event) {
    var saved = persistenceRepository.save(EdgeEventPersistenceAssembler.toPersistenceEntityFromDomain(event));
    return EdgeEventPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<EdgeEvent> findByEventUid(UUID eventUid) {
    if (eventUid == null) {
      return Optional.empty();
    }
    return persistenceRepository.findByEventUid(eventUid)
        .map(EdgeEventPersistenceAssembler::toDomainFromPersistenceEntity);
  }
}
