package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpOwnerCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpOwnerResource;

/**
 * Assembler that converts a {@link SignUpOwnerResource} into a {@link SignUpOwnerCommand}.
 */
public class SignUpOwnerCommandFromResourceAssembler {

  /**
   * Converts the owner sign-up payload into its command representation.
   *
   * @param resource the {@link SignUpOwnerResource} resource to convert
   * @return the {@link SignUpOwnerCommand} command
   * @throws IllegalArgumentException when a required value is missing
   */
  public static SignUpOwnerCommand toCommandFromResource(SignUpOwnerResource resource) {
    var contactDetails = new ContactDetails(
        resource.phone(),
        resource.street(),
        resource.number(),
        resource.city(),
        resource.postalCode(),
        resource.country());
    return new SignUpOwnerCommand(
        resource.username(),
        resource.password(),
        resource.email(),
        resource.fullName(),
        contactDetails,
        resource.ruc());
  }
}
