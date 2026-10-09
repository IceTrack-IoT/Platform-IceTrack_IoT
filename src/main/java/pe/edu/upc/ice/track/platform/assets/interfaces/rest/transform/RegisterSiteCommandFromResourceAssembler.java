package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterSiteResource;

/**
 * Assembler that converts a {@link RegisterSiteResource} into a {@link RegisterSiteCommand}.
 */
public class RegisterSiteCommandFromResourceAssembler {

  /**
   * Converts the site registration payload into its command representation.
   *
   * @param ownerId identifier of the authenticated owner
   * @param resource the {@link RegisterSiteResource} payload
   * @return the {@link RegisterSiteCommand} command
   */
  public static RegisterSiteCommand toCommandFromResource(Long ownerId, RegisterSiteResource resource) {
    return new RegisterSiteCommand(
        ownerId, resource.name(), resource.address(), resource.contactName(), resource.phone());
  }
}
