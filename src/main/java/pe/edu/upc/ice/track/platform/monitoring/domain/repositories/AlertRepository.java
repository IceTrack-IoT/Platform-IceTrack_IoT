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

  Alert save(Alert alert);

  Optional<Alert> findById(Long alertId);

  List<Alert> findByEquipmentIdAndStatusIn(Long equipmentId, List<AlertStatus> statuses);

  Optional<Alert> findOpenAlertByEquipmentIdAndType(
      Long equipmentId, pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType type);
}
