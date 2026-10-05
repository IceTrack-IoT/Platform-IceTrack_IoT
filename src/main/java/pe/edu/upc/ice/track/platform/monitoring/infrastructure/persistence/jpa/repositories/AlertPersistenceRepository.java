package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

import java.util.List;
import java.util.Optional;

/** Spring Data JPA repository operating on {@link AlertPersistenceEntity}. */
public interface AlertPersistenceRepository extends JpaRepository<AlertPersistenceEntity, Long> {

  /**
   *  Finds all alerts for a given equipment ID and a list of statuses.
   * @param equipmentId The equipment Id
   * @param statuses  The list of statuses to filter the alerts by.
   * @return  A list of {@link AlertPersistenceEntity} matching the criteria.
   */
  List<AlertPersistenceEntity> findByEquipmentIdAndStatusIn(Long equipmentId, List<String> statuses);

  /**
   *  Finds the first alert for a given equipment ID, type, and a list of open statuses.
   * @param equipmentId The equipment Id
   * @param type  The type of the alert
   * @param openStatuses  The list of open statuses to filter the alerts by.
   * @return An {@link Optional} containing the first {@link AlertPersistenceEntity} matching the criteria, or empty if none found.
   */
  Optional<AlertPersistenceEntity> findFirstByEquipmentIdAndTypeAndStatusIn(
      Long equipmentId, String type, List<String> openStatuses);
}
