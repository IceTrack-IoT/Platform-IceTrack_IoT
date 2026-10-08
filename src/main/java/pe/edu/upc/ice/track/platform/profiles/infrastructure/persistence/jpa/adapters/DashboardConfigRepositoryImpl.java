package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

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

  public DashboardConfigRepositoryImpl(DashboardConfigPersistenceRepository dashboardConfigPersistenceRepository) {
    this.dashboardConfigPersistenceRepository = dashboardConfigPersistenceRepository;
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
    // Flushed so the identities of newly attached cards are assigned before mapping back.
    var savedEntity = dashboardConfigPersistenceRepository.saveAndFlush(entity);
    return DashboardConfigPersistenceAssembler.toDomainFromPersistence(savedEntity);
  }
}
