package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteResource;

/**
 * Assembler that converts an {@link UpdateSiteResource} into an {@link UpdateSiteInfoCommand}.
 */
public class UpdateSiteInfoCommandFromResourceAssembler {

  /**
   * Converts the site update payload into its command representation.
   *
   * @param siteId identifier of the site being updated, taken from the request path
   * @param ownerId identifier of the authenticated owner
   * @param resource the {@link UpdateSiteResource} payload
   * @return the {@link UpdateSiteInfoCommand} command
   */
  public static UpdateSiteInfoCommand toCommandFromResource(
      Long siteId, Long ownerId, UpdateSiteResource resource) {
    return new UpdateSiteInfoCommand(
        siteId, ownerId, resource.name(), resource.address(), resource.contactName(), resource.phone());
  }
}
