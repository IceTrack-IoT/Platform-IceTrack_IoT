package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetMaintenanceWindowsByEquipmentQuery;

import java.util.List;
import java.util.Optional;

/** Application service handling read operations on {@link MaintenanceWindow}. */
public interface MaintenanceWindowQueryService {

  /**
   * Lists the maintenance windows of a unit the caller owns.
   *
   * @param query the unit, the optional period and the caller
   * @return the windows, newest first; empty when the unit does not exist or belongs to somebody else
   */
  Optional<List<MaintenanceWindow>> handle(GetMaintenanceWindowsByEquipmentQuery query);
}
