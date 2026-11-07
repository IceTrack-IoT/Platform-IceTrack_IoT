package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;

import java.util.List;
import java.util.Optional;

/** Application service handling read operations on {@link SensorReading}. */
public interface SensorReadingQueryService {

  /**
   * @param query the query naming the equipment, the range and the caller
   * @return the readings recorded within the range, or empty when the caller may not read the
   *         equipment (answered as 404 so foreign identifiers cannot be harvested)
   */
  Optional<List<SensorReading>> handle(GetReadingsByEquipmentQuery query);
}
