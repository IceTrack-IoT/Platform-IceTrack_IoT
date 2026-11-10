package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.MaintenanceWindowPersistenceEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository operating on {@link MaintenanceWindowPersistenceEntity}.
 *
 * <p>The coverage lookup and the listing with optional bounds go through
 * {@link JpaSpecificationExecutor} with the specifications of
 * {@code MaintenanceWindowPersistenceSpecifications}.</p>
 */
public interface MaintenanceWindowPersistenceRepository
    extends JpaRepository<MaintenanceWindowPersistenceEntity, Long>,
    JpaSpecificationExecutor<MaintenanceWindowPersistenceEntity> {

  /**
   * @param startEventUid identifier of the gateway event that started the window
   * @return the row, or empty when no window was started by that event
   */
  Optional<MaintenanceWindowPersistenceEntity> findByStartEventUid(UUID startEventUid);

  /**
   * @param equipmentId the unit
   * @return the latest started window of the unit that is still active, or empty
   */
  Optional<MaintenanceWindowPersistenceEntity> findFirstByEquipmentIdAndEndedAtIsNullOrderByStartedAtDesc(
      Long equipmentId);

  /**
   * @param cutoff the instant the expiration must precede
   * @return the active windows that expired before the cutoff, oldest expiration first
   */
  List<MaintenanceWindowPersistenceEntity> findByEndedAtIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(Instant cutoff);
}
