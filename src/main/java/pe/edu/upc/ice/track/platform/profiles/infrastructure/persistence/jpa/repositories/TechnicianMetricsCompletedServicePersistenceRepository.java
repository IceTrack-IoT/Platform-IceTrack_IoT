package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianMetricsCompletedServicePersistenceEntity;

/** Spring Data JPA repository operating on {@link TechnicianMetricsCompletedServicePersistenceEntity}. */
public interface TechnicianMetricsCompletedServicePersistenceRepository
    extends JpaRepository<TechnicianMetricsCompletedServicePersistenceEntity, Long> {
}
