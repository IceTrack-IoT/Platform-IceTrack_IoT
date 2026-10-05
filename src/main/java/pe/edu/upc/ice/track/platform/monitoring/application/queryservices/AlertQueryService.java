package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;

import java.util.List;
import java.util.Optional;

/** Application service handling read operations on {@link Alert}. */
public interface AlertQueryService {

  /**
   *  Fetches a single alert by its identifier.
   * @param query the query containing the alert identifier
   * @return  an {@link Optional} containing the alert if found, or empty if not found
   */
  Optional<Alert> handle(GetAlertByIdQuery query);

  /**
   *  Fetches all open alerts for a specific equipment.
   * @param query the query containing the equipment identifier
   * @return  a list of open alerts for the specified equipment
   */
  List<Alert> handle(GetOpenAlertsByEquipmentQuery query);
}
