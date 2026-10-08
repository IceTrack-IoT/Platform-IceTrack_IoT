package pe.edu.upc.ice.track.platform.profiles.application.commandservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.AddCardToDashboardCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.InitializeDashboardConfigCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.RemoveCardFromDashboardCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ToggleCardVisibilityCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardDefaultsCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Dashboard Config Command Service
 *
 * <p>Every command loads the whole {@link DashboardConfig} aggregate, applies the change through
 * its domain methods and saves it back: dashboard cards are never changed on their own.</p>
 */
public interface DashboardConfigCommandService {

  /**
   * Handle Initialize Dashboard Config Command.
   *
   * @param command The {@link InitializeDashboardConfigCommand} Command
   * @return A {@link Result} containing the created {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when a value is invalid or the account already has a configuration
   */
  Result<DashboardConfig, ApplicationError> handle(InitializeDashboardConfigCommand command);

  /**
   * Handle Add Card To Dashboard Command.
   *
   * @param command The {@link AddCardToDashboardCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration is missing, a value is invalid
   *         or the dashboard already shows a card of that type
   */
  Result<DashboardConfig, ApplicationError> handle(AddCardToDashboardCommand command);

  /**
   * Handle Toggle Card Visibility Command.
   *
   * @param command The {@link ToggleCardVisibilityCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration or the card is missing
   */
  Result<DashboardConfig, ApplicationError> handle(ToggleCardVisibilityCommand command);

  /**
   * Handle Remove Card From Dashboard Command.
   *
   * @param command The {@link RemoveCardFromDashboardCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration or the card is missing
   */
  Result<DashboardConfig, ApplicationError> handle(RemoveCardFromDashboardCommand command);

  /**
   * Handle Update Dashboard Defaults Command.
   *
   * @param command The {@link UpdateDashboardDefaultsCommand} Command
   * @return A {@link Result} containing the updated {@link DashboardConfig} on success,
   *         or an {@link ApplicationError} when the configuration is missing or a value is invalid
   */
  Result<DashboardConfig, ApplicationError> handle(UpdateDashboardDefaultsCommand command);
}
