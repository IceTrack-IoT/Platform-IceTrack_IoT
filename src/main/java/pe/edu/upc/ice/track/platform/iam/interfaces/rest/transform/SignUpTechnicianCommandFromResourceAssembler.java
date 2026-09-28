package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpTechnicianCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpTechnicianResource;

/**
 * Assembler that converts a {@link SignUpTechnicianResource} into a {@link SignUpTechnicianCommand}.
 */
public class SignUpTechnicianCommandFromResourceAssembler {

  /**
   * Converts the technician sign-up payload into its command representation.
   *
   * @param resource the {@link SignUpTechnicianResource} resource to convert
   * @return the {@link SignUpTechnicianCommand} command
   * @throws IllegalArgumentException when a required value is missing
   */
  public static SignUpTechnicianCommand toCommandFromResource(SignUpTechnicianResource resource) {
    var contactDetails = new ContactDetails(
        resource.phone(),
        resource.street(),
        resource.number(),
        resource.city(),
        resource.postalCode(),
        resource.country());
    return new SignUpTechnicianCommand(
        resource.username(),
        resource.password(),
        resource.email(),
        resource.fullName(),
        contactDetails,
        resource.speciality(),
        resource.certificationNumber());
  }
}
