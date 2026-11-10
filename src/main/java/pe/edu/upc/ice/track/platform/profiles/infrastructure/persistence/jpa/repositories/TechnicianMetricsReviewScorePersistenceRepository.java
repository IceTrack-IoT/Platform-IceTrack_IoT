package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianMetricsReviewScorePersistenceEntity;

/** Spring Data JPA repository operating on {@link TechnicianMetricsReviewScorePersistenceEntity}. */
public interface TechnicianMetricsReviewScorePersistenceRepository
    extends JpaRepository<TechnicianMetricsReviewScorePersistenceEntity, Long> {
}
