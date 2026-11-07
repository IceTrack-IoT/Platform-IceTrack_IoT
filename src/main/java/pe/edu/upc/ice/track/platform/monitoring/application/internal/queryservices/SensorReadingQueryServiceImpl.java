package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.SensorReadingQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link SensorReadingQueryService}, scoped by {@link EquipmentAccessPolicy}.
 */
@Service
public class SensorReadingQueryServiceImpl implements SensorReadingQueryService {

  private final SensorReadingRepository sensorReadingRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;

  public SensorReadingQueryServiceImpl(
      SensorReadingRepository sensorReadingRepository, EquipmentAccessPolicy equipmentAccessPolicy) {
    this.sensorReadingRepository = sensorReadingRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<List<SensorReading>> handle(GetReadingsByEquipmentQuery query) {
    if (!equipmentAccessPolicy.canRead(query.equipmentId(), query.username(), query.callerRole())) {
      return Optional.empty();
    }
    return Optional.of(sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(
        query.equipmentId(), query.from(), query.to()));
  }
}
