package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

import java.time.LocalDateTime;
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

  /**
   *  Counts the number of alerts for a given equipment ID and alert type within a specified time range.
   * @param equipmentId the identifier of the equipment for which to count alerts
   * @param type  the type of the alerts to be counted
   * @param from  the start of the time range for which to count alerts
   * @param to  the end of the time range for which to count alerts
   * @return  the number of alerts matching the given equipment ID, type, and time range
   */
  long countByEquipmentIdAndTypeInRange(Long equipmentId, AlertType type, LocalDateTime from, LocalDateTime to);

  /**
   *  Calculates the total duration in hours of alerts for a given equipment ID and alert type within a specified time range.
   * @param equipmentId the identifier of the equipment for which to calculate the total duration of alerts
   * @param type  the type of the alerts for which to calculate the total duration
   * @param from  the start of the time range for which to calculate the total duration
   * @param to  the end of the time range for which to calculate the total duration
   * @return  the total duration in hours of alerts matching the given equipment ID, type, and time range
   */
  double totalDurationHoursByEquipmentIdAndTypeInRange(Long equipmentId, AlertType type, LocalDateTime from, LocalDateTime to);
}
