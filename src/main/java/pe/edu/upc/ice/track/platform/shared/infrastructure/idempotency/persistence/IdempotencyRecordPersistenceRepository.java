package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Spring Data repository of the idempotency records. Purging expired records is a Criteria API bulk
 * delete built by {@link IdempotencyRecordPersistenceSpecifications}.
 */
public interface IdempotencyRecordPersistenceRepository
    extends JpaRepository<IdempotencyRecordPersistenceEntity, Long>,
    JpaSpecificationExecutor<IdempotencyRecordPersistenceEntity> {

  /**
   * Finds the record of a key a caller sent to an endpoint.
   *
   * @param caller         username of the caller
   * @param endpoint       method and path of the endpoint
   * @param idempotencyKey the key
   * @return the record, or empty when the key was never sent there by that caller
   */
  Optional<IdempotencyRecordPersistenceEntity> findByCallerAndEndpointAndIdempotencyKey(
      String caller, String endpoint, String idempotencyKey);
}
