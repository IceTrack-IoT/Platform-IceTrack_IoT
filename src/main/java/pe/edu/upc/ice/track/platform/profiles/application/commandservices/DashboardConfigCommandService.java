package pe.edu.upc.ice.track.platform.profiles.application.commandservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.InitializeDashboardConfigCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ResetDashboardConfigToDefaultCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ToggleCardVisibilityCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardDefaultsCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardLayoutCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Dashboard Config Command Service
 *
 * <p>Every command loads the whole {@link DashboardConfig} aggregate, applies the change through
 * its domain methods and saves it back: dashboard cards are never changed on their own, and never
 * added or deleted after the configuration is created.</p>
 */
public interface DashboardConfigCommandService {

  /**
   * Handle Initialize Dashboard Config Command.
   *
   * @param command The {@link InitializeDashboardConfigCommand} Command
   * @return A {@link Result} containing the created {@link DashboardConfig}, in the default card
   *         layout, on success, or an {@link ApplicationError} when a value is invalid or the
   *         account already has a configuration
   */
  Result<DashboardConfig, ApplicationError> handle(InitializeDashboardConfigCommand command);

  /**
   * Handle Update Dashboard Layout Command.
   *
   * @param command The {@link UpdateDashboardLayoutCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration is missing or the layout does
   *         not place every card exactly once at the positions {@code 1..N}
   */
  Result<DashboardConfig, ApplicationError> handle(UpdateDashboardLayoutCommand command);

  /**
   * Handle Toggle Card Visibility Command.
   *
   * @param command The {@link ToggleCardVisibilityCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration or the card is missing
   */
  Result<DashboardConfig, ApplicationError> handle(ToggleCardVisibilityCommand command);

  /**
   * Handle Reset Dashboard Config To Default Command.
   *
   * @param command The {@link ResetDashboardConfigToDefaultCommand} Command
   * @return A {@link Result} containing the {@link DashboardConfig}, back in the default card
   *         layout, on success, or an {@link ApplicationError} when the configuration is missing
   */
  Result<DashboardConfig, ApplicationError> handle(ResetDashboardConfigToDefaultCommand command);

  /**
   * Handle Update Dashboard Defaults Command.
   *
   * @param command The {@link UpdateDashboardDefaultsCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration is missing or a value is invalid
   */
  Result<DashboardConfig, ApplicationError> handle(UpdateDashboardDefaultsCommand command);
}
