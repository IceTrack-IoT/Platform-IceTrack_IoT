package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ReviewPersistenceEntity;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections.ReviewScoresProjection;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections.ServiceRequestIdProjection;

import java.util.Collection;
import java.util.List;
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

  /**
   * Finds which of the given service requests have been reviewed. The closed
   * {@link ServiceRequestIdProjection} limits the selection to the request identifier.
   *
   * @param serviceRequestIds the requests; required and not empty
   * @return one row per reviewed request among them
   */
  List<ServiceRequestIdProjection> findByServiceRequestIdIn(Collection<Long> serviceRequestIds);

  /**
   * Finds the scores of every review about a technician. The closed
   * {@link ReviewScoresProjection} limits the selection to the three score columns; the caller
   * computes the average from them.
   *
   * @param technicianId the technician
   * @return the scores of each review, empty when the technician has never been reviewed
   */
  List<ReviewScoresProjection> findScoresByTechnicianId(Long technicianId);
}
