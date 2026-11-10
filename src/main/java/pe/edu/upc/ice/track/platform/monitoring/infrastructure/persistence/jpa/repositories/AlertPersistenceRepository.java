package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository operating on {@link AlertPersistenceEntity}.
 *
 * <p>The owner-wide listing is served through {@link JpaSpecificationExecutor} with the
 * specification built by {@code AlertPersistenceSpecifications}: its filters are optional and
 * combine freely, which a derived query name cannot express.</p>
 */
public interface AlertPersistenceRepository
    extends JpaRepository<AlertPersistenceEntity, Long>, JpaSpecificationExecutor<AlertPersistenceEntity> {

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

  /**
   *  Finds all alerts for a given equipment ID, type, and a date range.
   * @param equipmentId The equipment Id
   * @param type  The type of the alert
   * @param from  The start of the date range (inclusive)
   * @param to  The end of the date range (inclusive)
   * @return  A list of {@link AlertPersistenceEntity} matching the criteria.
   */
  List<AlertPersistenceEntity> findByEquipmentIdAndTypeAndOpenedAtBetween(
      Long equipmentId, String type, Instant from, Instant to);

  /**
   *  Finds all alerts of several equipment units that hold one of the given statuses.
   * @param equipmentIds The identifiers of the equipment units
   * @param statuses  The names of the statuses to filter the alerts by.
   * @return  A list of {@link AlertPersistenceEntity} matching the criteria.
   */
  List<AlertPersistenceEntity> findByEquipmentIdInAndStatusIn(Collection<Long> equipmentIds, Collection<String> statuses);
}
