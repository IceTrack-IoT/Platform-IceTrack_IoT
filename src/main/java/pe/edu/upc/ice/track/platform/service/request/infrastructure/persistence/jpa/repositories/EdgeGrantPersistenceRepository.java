package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.EdgeGrantPersistenceEntity;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data repository for {@link EdgeGrantPersistenceEntity}.
 */
@Repository
public interface EdgeGrantPersistenceRepository extends JpaRepository<EdgeGrantPersistenceEntity, Long> {

  /**
   * Finds the grants of a request that have not been revoked.
   *
   * @param serviceRequestId the request identifier
   * @return the unrevoked rows, empty when there are none
   */
  List<EdgeGrantPersistenceEntity> findByServiceRequestIdAndRevokedAtIsNull(Long serviceRequestId);

  /**
   * Finds the grants of a unit that are neither revoked nor expired at an instant.
   *
   * @param equipmentId the unit identifier
   * @param at          the instant the grants must still be valid after
   * @return the active rows, oldest first
   */
  List<EdgeGrantPersistenceEntity> findByEquipmentIdAndRevokedAtIsNullAndExpiresAtAfterOrderByIdAsc(
      Long equipmentId, Instant at);
}
