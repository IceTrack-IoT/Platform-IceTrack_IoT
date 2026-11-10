package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertPage;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
   *  Lists the alerts of a given equipment ID and alert type opened within a specified time range, whatever their status.
   * @param equipmentId the identifier of the equipment
   * @param type  the type of the alerts
   * @param from  the start of the time range (inclusive)
   * @param to  the end of the time range (inclusive)
   * @return  the matching alerts; callers decide how DISMISSED alerts are treated
   */
  List<Alert> findByEquipmentIdAndTypeOpenedBetween(Long equipmentId, AlertType type, Instant from, Instant to);

  /**
   * Lists the alerts of several units that hold one of the given statuses.
   *
   * @param equipmentIds identifiers of the units; empty matches nothing
   * @param statuses     the statuses that count; empty matches nothing
   * @return the matching alerts, in no particular order
   */
  List<Alert> findByEquipmentIdInAndStatusIn(Collection<Long> equipmentIds, Collection<AlertStatus> statuses);

  /**
   * Finds one page of the alerts of several units, most recently opened first, together with the
   * number of alerts matching across every page.
   *
   * <p>An alert carries no owner: it belongs to whoever owns the unit it was raised on, and units
   * live in another context. The caller resolves the units of the owner first and passes their
   * identifiers here, so they are the scope of the listing and the filters can only narrow it.</p>
   *
   * @param equipmentIds identifiers of the units that scope the listing; empty matches nothing
   * @param statuses     restrict to these statuses; empty does not restrict
   * @param types        restrict to these kinds of alert; empty does not restrict
   * @param severities   restrict to these severities; empty does not restrict
   * @param openedFrom   inclusive lower bound of the opening instant, may be {@code null}
   * @param openedTo     inclusive upper bound of the opening instant, may be {@code null}
   * @param page         zero-based page index; not negative
   * @param size         maximum number of alerts the page may hold; strictly positive
   * @return the alerts of that page and the total count
   */
  AlertPage findPageByEquipmentIdIn(
      Collection<Long> equipmentIds, Set<AlertStatus> statuses, Set<AlertType> types, Set<AlertSeverity> severities,
      Instant openedFrom, Instant openedTo, int page, int size);
}
