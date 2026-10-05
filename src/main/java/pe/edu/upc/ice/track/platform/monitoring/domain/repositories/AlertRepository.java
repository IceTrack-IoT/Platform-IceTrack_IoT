package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository port for {@link Alert}.
 *
 * <p>Implemented by {@code AlertRepositoryImpl} in the infrastructure layer, which delegates to
 * the Spring Data JPA repository and the persistence assembler.</p>
 */
public interface AlertRepository {

  /**
   *  Persists the given alert in the database.
   * @param alert the alert to be persisted
   * @return  the persisted alert with any generated values (e.g., ID)
   */
  Alert save(Alert alert);

  /**
   *  Retrieves an alert by its identifier.
   * @param alertId the identifier of the alert to be retrieved
   * @return  an {@code Optional} containing the alert if found, or empty if not found
   */
  Optional<Alert> findById(Long alertId);

  /**
   *  Retrieves a list of alerts for a given equipment ID and a list of alert statuses.
   * @param equipmentId the identifier of the equipment for which to retrieve alerts
   * @param statuses  the list of alert statuses to filter the alerts
   * @return  a list of alerts matching the given equipment ID and statuses
   */
  List<Alert> findByEquipmentIdAndStatusIn(Long equipmentId, List<AlertStatus> statuses);

  /**
   *  Retrieves an open alert for a given equipment ID and alert type.
   * @param equipmentId the identifier of the equipment for which to retrieve the open alert
   * @param type  the type of the alert to be retrieved
   * @return  an {@code Optional} containing the open alert if found, or empty if not found
   */
  Optional<Alert> findOpenAlertByEquipmentIdAndType(
      Long equipmentId, pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType type);
}
