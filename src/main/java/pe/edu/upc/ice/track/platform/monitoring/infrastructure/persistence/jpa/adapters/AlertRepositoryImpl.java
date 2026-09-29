package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.AlertPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.AlertPersistenceRepository;

import java.util.List;
import java.util.Optional;

/** Adapter fulfilling {@link AlertRepository} on top of Spring Data JPA. */
@Repository
public class AlertRepositoryImpl implements AlertRepository {

  private final AlertPersistenceRepository persistenceRepository;

  public AlertRepositoryImpl(AlertPersistenceRepository persistenceRepository) {
    this.persistenceRepository = persistenceRepository;
  }

  @Override
  public Alert save(Alert alert) {
    var entity = AlertPersistenceAssembler.toPersistenceEntityFromDomain(alert);
    var saved = persistenceRepository.save(entity);
    return AlertPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<Alert> findById(Long alertId) {
    return persistenceRepository.findById(alertId)
        .map(AlertPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public List<Alert> findByEquipmentIdAndStatusIn(Long equipmentId, List<AlertStatus> statuses) {
    var statusNames = statuses.stream().map(Enum::name).toList();
    return persistenceRepository.findByEquipmentIdAndStatusIn(equipmentId, statusNames).stream()
        .map(AlertPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  public Optional<Alert> findOpenAlertByEquipmentIdAndType(Long equipmentId, AlertType type) {
    var openStatuses = List.of(AlertStatus.OPEN.name(), AlertStatus.ACKNOWLEDGED.name());
    return persistenceRepository
        .findFirstByEquipmentIdAndTypeAndStatusIn(equipmentId, type.name(), openStatuses)
        .map(AlertPersistenceAssembler::toDomainFromPersistenceEntity);
  }
}
