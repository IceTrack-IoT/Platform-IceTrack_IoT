package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;

/**
 * Assembler that converts an {@link UpdateSiteResource} into an {@link UpdateSiteInfoCommand}.
 */
public class UpdateSiteInfoCommandFromResourceAssembler {

  private UpdateSiteInfoCommandFromResourceAssembler() {
  }

  /**
   * Converts the site update payload into its command representation.
   *
   * @param siteId   identifier of the site, taken from the request path
   * @param username username of the authenticated caller
   * @param resource the {@link UpdateSiteResource} payload
   * @param ifMatch  the raw {@code If-Match} header, or {@code null}
   * @return the {@link UpdateSiteInfoCommand} command
   */
  public static UpdateSiteInfoCommand toCommandFromResource(
      Long siteId, String username, UpdateSiteResource resource, String ifMatch) {
    return new UpdateSiteInfoCommand(
        siteId, username, resource.name(), resource.contactName(), resource.phone(),
        IfMatchHeaderAssembler.toExpectedVersion(ifMatch));
  }
}
