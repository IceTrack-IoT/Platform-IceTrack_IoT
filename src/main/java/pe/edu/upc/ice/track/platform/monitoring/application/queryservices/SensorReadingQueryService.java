package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;

import java.util.List;

/** Application service handling read operations on {@link SensorReading}. */
public interface SensorReadingQueryService {

  List<SensorReading> handle(GetReadingsByEquipmentQuery query);
}
