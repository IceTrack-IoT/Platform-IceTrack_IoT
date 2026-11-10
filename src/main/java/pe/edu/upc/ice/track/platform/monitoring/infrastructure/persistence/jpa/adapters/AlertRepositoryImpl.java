package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertPage;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.AlertPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.AlertPersistenceRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.specifications.AlertPersistenceSpecifications;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Adapter fulfilling {@link AlertRepository} on top of Spring Data JPA. */
@Repository
public class AlertRepositoryImpl implements AlertRepository {

  /**
   * Most recently opened first, then by identifier, because a listing paginated without a total
   * order silently drops and repeats rows from one page to the next.
   */
  private static final Sort LISTING_ORDER = Sort.by(Sort.Order.desc("openedAt"), Sort.Order.desc("id"));

  private final AlertPersistenceRepository persistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public AlertRepositoryImpl(AlertPersistenceRepository persistenceRepository,
                             ApplicationEventPublisher eventPublisher) {
    this.persistenceRepository = persistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Alert save(Alert alert) {
    var entity = AlertPersistenceAssembler.toPersistenceEntityFromDomain(alert);
    var saved = persistenceRepository.save(entity);
    var savedAlert = AlertPersistenceAssembler.toDomainFromPersistenceEntity(saved);
    alert.domainEvents().forEach(eventPublisher::publishEvent);
    alert.clearDomainEvents();
    return savedAlert;
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

  @Override
  public List<Alert> findByEquipmentIdAndTypeOpenedBetween(
      Long equipmentId, AlertType type, Instant from, Instant to) {
    return persistenceRepository.findByEquipmentIdAndTypeAndOpenedAtBetween(equipmentId, type.name(), from, to).stream()
        .map(AlertPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  public List<Alert> findByEquipmentIdInAndStatusIn(Collection<Long> equipmentIds, Collection<AlertStatus> statuses) {
    if (equipmentIds == null || equipmentIds.isEmpty() || statuses == null || statuses.isEmpty()) {
      return List.of();
    }
    var statusNames = statuses.stream().map(Enum::name).toList();
    return persistenceRepository.findByEquipmentIdInAndStatusIn(equipmentIds, statusNames).stream()
        .map(AlertPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The rows and the count come from the same specification, so the total can never disagree
   * with the filters the rows were selected by. A page whose first row lies beyond what a JDBC
   * offset can address is necessarily past the end, so it is answered empty, with the real total.</p>
   */
  @Override
  public AlertPage findPageByEquipmentIdIn(
      Collection<Long> equipmentIds, Set<AlertStatus> statuses, Set<AlertType> types, Set<AlertSeverity> severities,
      Instant openedFrom, Instant openedTo, int page, int size) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return AlertPage.empty(page, size);
    }
    var specification = AlertPersistenceSpecifications.raisedOnEquipment(
        equipmentIds, statuses, types, severities, openedFrom, openedTo);
    if ((long) page * size > Integer.MAX_VALUE) {
      return new AlertPage(List.of(), page, size, persistenceRepository.count(specification));
    }
    var slice = persistenceRepository.findAll(specification, PageRequest.of(page, size, LISTING_ORDER));
    var content = slice.getContent().stream()
        .map(AlertPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
    return new AlertPage(content, page, size, slice.getTotalElements());
  }
}
