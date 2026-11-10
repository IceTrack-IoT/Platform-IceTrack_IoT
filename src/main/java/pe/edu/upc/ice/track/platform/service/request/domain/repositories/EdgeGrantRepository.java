package pe.edu.upc.ice.track.platform.service.request.domain.repositories;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;

import java.time.Instant;
import java.util.List;

/**
 * Edge grant repository port.
 *
 * <p>Implemented by {@code EdgeGrantRepositoryImpl}, which also publishes the domain events a grant
 * registered once its row is written.</p>
 */
public interface EdgeGrantRepository {

  /**
   * Persist a grant, inserting it when it is new and updating it otherwise.
   *
   * @param grant the grant to persist
   * @return the persisted grant
   */
  EdgeGrant save(EdgeGrant grant);

  /**
   * Find the grants of a request that have not been revoked, expired or not.
   *
   * @param serviceRequestId the request identifier
   * @return the unrevoked grants, empty when there are none
   */
  List<EdgeGrant> findUnrevokedByServiceRequestId(Long serviceRequestId);

  /**
   * Find the grants that open a unit's gateway at a given instant: not revoked and not expired.
   *
   * @param equipmentId the unit identifier
   * @param at          the instant to test against
   * @return the active grants, oldest first
   */
  List<EdgeGrant> findActiveByEquipmentId(Long equipmentId, Instant at);
}
