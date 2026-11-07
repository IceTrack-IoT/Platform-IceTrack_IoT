package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ValidateAddressCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ValidateAddressResource;

/**
 * Assembler that converts a {@link ValidateAddressResource} into a {@link ValidateAddressCommand}.
 */
public class ValidateAddressCommandFromResourceAssembler {

  private ValidateAddressCommandFromResourceAssembler() {
  }

  /**
   * Converts the address validation payload into its command representation.
   *
   * @param username username of the authenticated caller
   * @param resource the {@link ValidateAddressResource} payload
   * @return the {@link ValidateAddressCommand} command
   */
  public static ValidateAddressCommand toCommandFromResource(String username, ValidateAddressResource resource) {
    return new ValidateAddressCommand(username, resource.address(), resource.placeId());
  }
}
