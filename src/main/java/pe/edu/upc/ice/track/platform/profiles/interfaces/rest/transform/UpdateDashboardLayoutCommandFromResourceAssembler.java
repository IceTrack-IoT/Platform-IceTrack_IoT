package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CardLayoutCommandItem;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardLayoutCommand;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CardLayoutItemResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardLayoutResource;

/**
 * Assembler that converts an {@link UpdateDashboardLayoutResource} into an
 * {@link UpdateDashboardLayoutCommand}.
 */
public class UpdateDashboardLayoutCommandFromResourceAssembler {

  /**
   * Converts the dashboard layout update payload into its command representation.
   *
   * @param userId   identifier of the account owning the dashboard, taken from the request path
   * @param resource the {@link UpdateDashboardLayoutResource} resource to convert
   * @return the {@link UpdateDashboardLayoutCommand} command
   */
  public static UpdateDashboardLayoutCommand toCommandFromResource(Long userId, UpdateDashboardLayoutResource resource) {
    return new UpdateDashboardLayoutCommand(
        userId,
        resource.cards().stream()
            .map(UpdateDashboardLayoutCommandFromResourceAssembler::toCommandItemFromResource)
            .toList());
  }

  private static CardLayoutCommandItem toCommandItemFromResource(CardLayoutItemResource resource) {
    return new CardLayoutCommandItem(resource.cardId(), resource.order(), resource.isVisible());
  }
}
