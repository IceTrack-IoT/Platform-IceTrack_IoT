package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianMetricsProjectionPersistenceEntity;

/** Spring Data JPA repository operating on {@link TechnicianMetricsProjectionPersistenceEntity}. */
public interface TechnicianMetricsProjectionPersistenceRepository
    extends JpaRepository<TechnicianMetricsProjectionPersistenceEntity, Long> {
}
