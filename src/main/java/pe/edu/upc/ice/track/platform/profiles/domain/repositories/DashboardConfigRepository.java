package pe.edu.upc.ice.track.platform.profiles.domain.repositories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Dashboard configuration repository port.
 *
 * <p>The only way to load and save dashboard cards: they are internal entities of the
 * {@link DashboardConfig} aggregate, so there is deliberately no repository for them. A
 * configuration is always loaded and saved whole, together with its cards.</p>
 */
public interface DashboardConfigRepository {

  /**
   * Find a dashboard configuration by its identifier.
   *
   * @param dashboardConfigId the configuration identifier
   * @return the configuration with its cards, or empty when not found
   */
  Optional<DashboardConfig> findById(Long dashboardConfigId);

  /**
   * Find the dashboard configuration of a platform account.
   *
   * @param userId the identifier of the account
   * @return the configuration with its cards, or empty when the account has none
   */
  Optional<DashboardConfig> findByUserId(UserId userId);

  /**
   * Check whether a platform account already has a dashboard configuration.
   *
   * @param userId the identifier of the account
   * @return true when the account has a configuration
   */
  boolean existsByUserId(UserId userId);

  /**
   * Save a dashboard configuration together with its cards.
   *
   * <p>Cards no longer present in the configuration are deleted, new ones are inserted.</p>
   *
   * @param dashboardConfig the configuration to save
   * @return the saved configuration, carrying the identities assigned to new cards
   */
  DashboardConfig save(DashboardConfig dashboardConfig);
}
