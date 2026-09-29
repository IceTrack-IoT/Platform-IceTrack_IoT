package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;

import java.util.List;
import java.util.Optional;

/** Application service handling read operations on {@link Alert}. */
public interface AlertQueryService {

  Optional<Alert> handle(GetAlertByIdQuery query);

  List<Alert> handle(GetOpenAlertsByEquipmentQuery query);
}
