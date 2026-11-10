package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.MaintenanceWindowPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.MaintenanceWindowPersistenceEntity;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.MaintenanceWindowPersistenceRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.specifications.MaintenanceWindowPersistenceSpecifications;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter fulfilling {@link MaintenanceWindowRepository} on top of Spring Data JPA.
 *
 * <p>An existing window is saved by updating the managed entity it was loaded from. The identity of a
 * new window is copied back onto the aggregate that was passed in, and the events the change
 * registered are published right after the row is flushed, inside the caller's transaction.</p>
 */
@Repository
public class MaintenanceWindowRepositoryImpl implements MaintenanceWindowRepository {

  /** Newest first, then by identifier, so two windows started at the same instant keep a total order. */
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"));

  private final MaintenanceWindowPersistenceRepository persistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public MaintenanceWindowRepositoryImpl(MaintenanceWindowPersistenceRepository persistenceRepository,
                                         ApplicationEventPublisher eventPublisher) {
    this.persistenceRepository = persistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public MaintenanceWindow save(MaintenanceWindow window) {
    var entity = window.getId() == null
        ? new MaintenanceWindowPersistenceEntity()
        : persistenceRepository.findById(window.getId()).orElseThrow(() -> new IllegalStateException(
            "Maintenance window %s was deleted after it was read".formatted(window.getId())));
    MaintenanceWindowPersistenceAssembler.copyToPersistence(window, entity);
    var saved = persistenceRepository.saveAndFlush(entity);
    window.assignId(saved.getId());
    window.domainEvents().forEach(eventPublisher::publishEvent);
    window.clearDomainEvents();
    return MaintenanceWindowPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<MaintenanceWindow> findByStartEventUid(UUID startEventUid) {
    if (startEventUid == null) {
      return Optional.empty();
    }
    return persistenceRepository.findByStartEventUid(startEventUid)
        .map(MaintenanceWindowPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public Optional<MaintenanceWindow> findActiveByEquipmentId(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return persistenceRepository.findFirstByEquipmentIdAndEndedAtIsNullOrderByStartedAtDesc(equipmentId)
        .map(MaintenanceWindowPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public Optional<MaintenanceWindow> findCovering(Long equipmentId, Instant timestamp) {
    if (equipmentId == null || timestamp == null) {
      return Optional.empty();
    }
    return persistenceRepository
        .findAll(MaintenanceWindowPersistenceSpecifications.covering(equipmentId, timestamp), NEWEST_FIRST)
        .stream()
        .findFirst()
        .map(MaintenanceWindowPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public List<MaintenanceWindow> findByEquipmentIdAndStartedAtBetween(Long equipmentId, Instant from, Instant to) {
    if (equipmentId == null) {
      return List.of();
    }
    return persistenceRepository
        .findAll(MaintenanceWindowPersistenceSpecifications.startedBetween(equipmentId, from, to), NEWEST_FIRST)
        .stream()
        .map(MaintenanceWindowPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  public List<MaintenanceWindow> findActiveExpiredBefore(Instant cutoff) {
    if (cutoff == null) {
      return List.of();
    }
    return persistenceRepository.findByEndedAtIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(cutoff).stream()
        .map(MaintenanceWindowPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }
}
