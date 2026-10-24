package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities.ReportPersistenceEntity;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ReportPersistenceEntity}.
 */
public interface ReportPersistenceRepository extends JpaRepository<ReportPersistenceEntity, Long> {

  /**
   *  Finds every report requested by a user, most recent first.
   * @param userId the identifier of the requesting user
   * @return the user's report entities, empty when they have none
   */
  List<ReportPersistenceEntity> findByUserIdOrderByRequestedAtDesc(Long userId);
}
