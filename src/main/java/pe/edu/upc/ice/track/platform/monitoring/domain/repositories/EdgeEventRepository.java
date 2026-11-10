package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository port for {@link EdgeEvent}.
 */
public interface EdgeEventRepository {

  /**
   * Persists a processed event. The unique index on {@code event_uid} rejects a second record of
   * the same event.
   *
   * @param event the processed event
   * @return the persisted event
   */
  EdgeEvent save(EdgeEvent event);

  /**
   * @param eventUid the identifier the gateway generated
   * @return the processed event, or empty when it has not been recorded
   */
  Optional<EdgeEvent> findByEventUid(UUID eventUid);
}
