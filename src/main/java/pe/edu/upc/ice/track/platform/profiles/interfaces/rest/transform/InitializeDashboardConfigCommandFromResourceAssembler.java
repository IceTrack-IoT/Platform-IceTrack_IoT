package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.InitializeDashboardConfigCommand;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CreateDashboardConfigResource;

/**
 * Assembler that converts a {@link CreateDashboardConfigResource} into an
 * {@link InitializeDashboardConfigCommand}.
 */
public class InitializeDashboardConfigCommandFromResourceAssembler {

  /**
   * Converts the dashboard configuration creation payload into its command representation.
   *
   * @param resource the {@link CreateDashboardConfigResource} resource to convert
   * @return the {@link InitializeDashboardConfigCommand} command
   */
  public static InitializeDashboardConfigCommand toCommandFromResource(CreateDashboardConfigResource resource) {
    return new InitializeDashboardConfigCommand(
        resource.userId(),
        resource.defaultSiteId(),
        resource.defaultTemperatureRange().min(),
        resource.defaultTemperatureRange().max(),
        resource.defaultTemperatureRange().unit(),
        resource.defaultTemperatureRange().label());
  }
}
