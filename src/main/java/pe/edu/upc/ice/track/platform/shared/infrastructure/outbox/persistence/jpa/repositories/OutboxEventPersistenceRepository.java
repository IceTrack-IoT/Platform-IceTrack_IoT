package pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.repositories;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.entities.OutboxEventPersistenceEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository operating on {@link OutboxEventPersistenceEntity}.
 */
public interface OutboxEventPersistenceRepository extends JpaRepository<OutboxEventPersistenceEntity, Long> {

  /**
   * Lists the next pending events, in the order they were appended.
   *
   * @param maxAttempts events that already failed this many times are left out
   * @return at most 100 pending events, oldest first
   */
  List<OutboxEventPersistenceEntity> findTop100ByPublishedAtIsNullAndAttemptsLessThanOrderByIdAsc(int maxAttempts);

  /**
   * Locks a pending event for delivery.
   *
   * <p>The row is locked {@code FOR UPDATE SKIP LOCKED} (a lock timeout of {@code -2}), so when
   * several instances dispatch at once, an event another instance is delivering is skipped rather
   * than waited for and delivered twice.</p>
   *
   * @param id the event identity
   * @return the locked event, or empty when it was published meanwhile or another instance holds it
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  Optional<OutboxEventPersistenceEntity> findByIdAndPublishedAtIsNull(Long id);
}
