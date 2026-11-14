package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteAddressResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;

/**
 * Assembler that converts an {@link UpdateSiteAddressResource} into an {@link UpdateSiteAddressCommand}.
 */
public class UpdateSiteAddressCommandFromResourceAssembler {

  private UpdateSiteAddressCommandFromResourceAssembler() {
  }

  /**
   * Converts the address change payload into its command representation.
   *
   * @param siteId   identifier of the site, taken from the request path
   * @param username username of the authenticated caller
   * @param resource the {@link UpdateSiteAddressResource} payload
   * @param ifMatch  the raw {@code If-Match} header, or {@code null}
   * @return the {@link UpdateSiteAddressCommand} command
   */
  public static UpdateSiteAddressCommand toCommandFromResource(
      Long siteId, String username, UpdateSiteAddressResource resource, String ifMatch) {
    var pin = resource.pin();
    return new UpdateSiteAddressCommand(
        siteId,
        username,
        resource.validationToken(),
        pin == null ? null : pin.latitude(),
        pin == null ? null : pin.longitude(),
        IfMatchHeaderAssembler.toExpectedVersion(ifMatch));
  }
}
