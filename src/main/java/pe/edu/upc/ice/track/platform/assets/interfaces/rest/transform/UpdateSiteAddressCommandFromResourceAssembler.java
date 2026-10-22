package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteAddressResource;

/**
 * Assembler that converts an {@link UpdateSiteAddressResource} into an
 * {@link UpdateSiteAddressCommand}.
 */
public class UpdateSiteAddressCommandFromResourceAssembler {

  /**
   * Converts the address update payload into its command representation.
   *
   * @param siteId identifier of the site being updated, taken from the request path
   * @param ownerId identifier of the authenticated owner
   * @param resource the {@link UpdateSiteAddressResource} payload
   * @return the {@link UpdateSiteAddressCommand} command
   */
  public static UpdateSiteAddressCommand toCommandFromResource(
      Long siteId, Long ownerId, UpdateSiteAddressResource resource) {
    return new UpdateSiteAddressCommand(siteId, ownerId, resource.address());
  }
}
