package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardDefaultsCommand;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardDefaultsResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;

/**
 * Assembler that converts an {@link UpdateDashboardDefaultsResource} into an
 * {@link UpdateDashboardDefaultsCommand}.
 */
public class UpdateDashboardDefaultsCommandFromResourceAssembler {

  /**
   * Converts the dashboard defaults update payload into its command representation.
   *
   * @param userId   identifier of the account owning the dashboard, taken from the request path
   * @param resource the {@link UpdateDashboardDefaultsResource} resource to convert
   * @param ifMatch  the raw {@code If-Match} header, or {@code null}
   * @return the {@link UpdateDashboardDefaultsCommand} command
   */
  public static UpdateDashboardDefaultsCommand toCommandFromResource(
      Long userId, UpdateDashboardDefaultsResource resource, String ifMatch) {
    return new UpdateDashboardDefaultsCommand(
        userId,
        resource.defaultSiteId(),
        resource.defaultTemperatureRange().min(),
        resource.defaultTemperatureRange().max(),
        resource.defaultTemperatureRange().unit(),
        resource.defaultTemperatureRange().label(),
        IfMatchHeaderAssembler.toExpectedVersion(ifMatch));
  }
}
