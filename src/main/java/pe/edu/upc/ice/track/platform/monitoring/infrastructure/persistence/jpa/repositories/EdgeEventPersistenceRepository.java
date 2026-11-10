package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.EdgeEventPersistenceEntity;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository operating on {@link EdgeEventPersistenceEntity}. */
public interface EdgeEventPersistenceRepository extends JpaRepository<EdgeEventPersistenceEntity, Long> {

  /**
   * Finds a processed event by the identifier its gateway generated.
   *
   * @param eventUid the identifier the gateway generated
   * @return the row, or empty when the event has not been recorded
   */
  Optional<EdgeEventPersistenceEntity> findByEventUid(UUID eventUid);
}
