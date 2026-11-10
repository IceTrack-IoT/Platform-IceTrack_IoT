package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities.ReportPersistenceEntity;

/**
 * Spring Data JPA repository for {@link ReportPersistenceEntity}.
 *
 * <p>A user's listings, plain or paged, go through {@link JpaSpecificationExecutor} with the
 * specification built by {@code ReportPersistenceSpecifications}.</p>
 */
public interface ReportPersistenceRepository
    extends JpaRepository<ReportPersistenceEntity, Long>,
    JpaSpecificationExecutor<ReportPersistenceEntity> {
}
