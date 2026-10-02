package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.SensorReadingQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.util.List;

@Service
public class SensorReadingQueryServiceImpl implements SensorReadingQueryService {

  private final SensorReadingRepository sensorReadingRepository;

  public SensorReadingQueryServiceImpl(SensorReadingRepository sensorReadingRepository) {
    this.sensorReadingRepository = sensorReadingRepository;
  }

  @Override
  public List<SensorReading> handle(GetReadingsByEquipmentQuery query) {
    return sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(
        query.equipmentId(), query.from(), query.to());
  }
}
