package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateOwnerResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;

/**
 * Assembler that converts an {@link UpdateOwnerResource} into an {@link UpdateOwnerCommand}.
 */
public class UpdateOwnerCommandFromResourceAssembler {

  /**
   * Converts the owner update payload into its command representation.
   *
   * <p>Each value object validates itself on construction; a violation surfaces as an
   * {@link IllegalArgumentException}, rendered as a 400 response.</p>
   *
   * @param ownerId  identifier of the owner to update, taken from the request path
   * @param resource the {@link UpdateOwnerResource} resource to convert
   * @param ifMatch  the raw {@code If-Match} header, or {@code null}
   * @return the {@link UpdateOwnerCommand} command
   * @throws IllegalArgumentException when a value violates a domain invariant, or the header is malformed
   */
  public static UpdateOwnerCommand toCommandFromResource(Long ownerId, UpdateOwnerResource resource, String ifMatch) {
    return new UpdateOwnerCommand(
        ownerId,
        PersonName.fromDisplayName(resource.fullName()),
        Phone.fromString(resource.phone()),
        new StreetAddress(resource.street(), resource.number(), resource.city(), resource.postalCode(), resource.country()),
        new Ruc(resource.ruc()),
        IfMatchHeaderAssembler.toExpectedVersion(ifMatch));
  }
}
