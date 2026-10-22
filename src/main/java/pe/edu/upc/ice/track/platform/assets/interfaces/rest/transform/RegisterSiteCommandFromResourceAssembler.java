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
   * @param username username of the authenticated caller
   * @param resource the {@link RegisterSiteResource} payload
   * @return the {@link RegisterSiteCommand} command
   */
  public static RegisterSiteCommand toCommandFromResource(String username, RegisterSiteResource resource) {
    return new RegisterSiteCommand(
        username, resource.name(), resource.address(), resource.contactName(), resource.phone());
  }
}
