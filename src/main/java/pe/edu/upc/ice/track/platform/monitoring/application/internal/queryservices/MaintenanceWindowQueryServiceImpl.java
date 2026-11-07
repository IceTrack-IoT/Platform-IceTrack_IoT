package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.MaintenanceWindowQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetMaintenanceWindowsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link MaintenanceWindowQueryService}.
 *
 * <p>Only the owner of the unit may audit its maintenance windows: ownership is checked against Asset
 * Management through {@link EquipmentAccessPolicy#canManage}, and a unit the caller does not own is
 * answered exactly like one that does not exist.</p>
 */
@Service
public class MaintenanceWindowQueryServiceImpl implements MaintenanceWindowQueryService {

  private final MaintenanceWindowRepository maintenanceWindowRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;

  public MaintenanceWindowQueryServiceImpl(MaintenanceWindowRepository maintenanceWindowRepository,
                                           EquipmentAccessPolicy equipmentAccessPolicy) {
    this.maintenanceWindowRepository = maintenanceWindowRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<List<MaintenanceWindow>> handle(GetMaintenanceWindowsByEquipmentQuery query) {
    if (!equipmentAccessPolicy.canManage(query.equipmentId(), query.username())) {
      return Optional.empty();
    }
    return Optional.of(maintenanceWindowRepository.findByEquipmentIdAndStartedAtBetween(
        query.equipmentId(), query.from(), query.to()));
  }
}
