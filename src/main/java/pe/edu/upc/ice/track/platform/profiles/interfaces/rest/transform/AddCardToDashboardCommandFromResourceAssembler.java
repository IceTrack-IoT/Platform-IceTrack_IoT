package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.AddCardToDashboardCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.AddCardResource;

/**
 * Assembler that converts an {@link AddCardResource} into an {@link AddCardToDashboardCommand}.
 */
public class AddCardToDashboardCommandFromResourceAssembler {

  /**
   * Converts the card addition payload into its command representation.
   *
   * <p>An unknown card type surfaces as an {@link IllegalArgumentException}, rendered as a 400
   * response.</p>
   *
   * @param userId   identifier of the account owning the dashboard, taken from the request path
   * @param resource the {@link AddCardResource} resource to convert
   * @return the {@link AddCardToDashboardCommand} command
   * @throws IllegalArgumentException when the card type is unknown
   */
  public static AddCardToDashboardCommand toCommandFromResource(Long userId, AddCardResource resource) {
    return new AddCardToDashboardCommand(
        userId,
        CardType.fromString(resource.cardType()),
        resource.order(),
        resource.isVisible());
  }
}
