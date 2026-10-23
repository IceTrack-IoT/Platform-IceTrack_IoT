package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ReviewPersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for {@link ReviewPersistenceEntity}.
 */
@Repository
public interface ReviewPersistenceRepository extends JpaRepository<ReviewPersistenceEntity, Long> {

  /**
   * Finds the review of a service request.
   *
   * @param serviceRequestId the reviewed request
   * @return the row, or empty when the request has not been reviewed
   */
  Optional<ReviewPersistenceEntity> findByServiceRequestId(Long serviceRequestId);

  /**
   * Tells whether a service request has already been reviewed.
   *
   * @param serviceRequestId the request
   * @return {@code true} when a review exists for it
   */
  boolean existsByServiceRequestId(Long serviceRequestId);
}
