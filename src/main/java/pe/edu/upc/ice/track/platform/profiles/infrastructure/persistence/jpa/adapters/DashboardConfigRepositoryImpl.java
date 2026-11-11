package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers.DashboardConfigPersistenceAssembler;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.DashboardConfigPersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.DashboardConfigPersistenceRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Repository adapter that bridges the dashboard configuration repository port with Spring Data JPA.
 *
 * <p>An existing configuration is saved by updating the managed entity it was loaded from, never by
 * merging a freshly built one: that keeps the managed {@code cards} collection in place, so orphan
 * removal deletes exactly the cards the aggregate dropped.</p>
 */
@Repository
public class DashboardConfigRepositoryImpl implements DashboardConfigRepository {

  private final DashboardConfigPersistenceRepository dashboardConfigPersistenceRepository;
  private final EntityManager entityManager;

  public DashboardConfigRepositoryImpl(
      DashboardConfigPersistenceRepository dashboardConfigPersistenceRepository, EntityManager entityManager) {
    this.dashboardConfigPersistenceRepository = dashboardConfigPersistenceRepository;
    this.entityManager = entityManager;
  }

  @Override
  public Optional<DashboardConfig> findById(Long dashboardConfigId) {
    if (dashboardConfigId == null) return Optional.empty();
    return dashboardConfigPersistenceRepository.findWithCardsById(dashboardConfigId)
        .map(DashboardConfigPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<DashboardConfig> findByUserId(UserId userId) {
    if (userId == null || userId.userId() == null) return Optional.empty();
    return dashboardConfigPersistenceRepository.findWithCardsByUserId(userId.userId())
        .map(DashboardConfigPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public boolean existsByUserId(UserId userId) {
    if (userId == null || userId.userId() == null) return false;
    return dashboardConfigPersistenceRepository.existsByUserId(userId.userId());
  }

  @Override
  @Transactional
  public DashboardConfig save(DashboardConfig dashboardConfig) {
    var entity = dashboardConfig.getDashboardConfigId() == null
        ? new DashboardConfigPersistenceEntity()
        : dashboardConfigPersistenceRepository.findWithCardsById(dashboardConfig.getDashboardConfigId())
            .orElseThrow(() -> new IllegalStateException(
                "Dashboard configuration %s no longer exists".formatted(dashboardConfig.getDashboardConfigId())));
    DashboardConfigPersistenceAssembler.copyToPersistence(dashboardConfig, entity);
    if (dashboardConfig.getDashboardConfigId() != null) {
      // Card changes live in the child rows, which would leave the version untouched; every save of an
      // existing configuration is a change the client must see as a new version. The pessimistic variant
      // increments at once, so the copy answered below already carries the new version.
      entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }
    // Flushed so the identities of newly attached cards, and the new version, are assigned before mapping back.
    var savedEntity = dashboardConfigPersistenceRepository.saveAndFlush(entity);
    return DashboardConfigPersistenceAssembler.toDomainFromPersistence(savedEntity);
  }
}
