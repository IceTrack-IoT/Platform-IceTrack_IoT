package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleTechnicianRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CompleteGoogleTechnicianRegistrationResource;

/**
 * Assembler that converts a {@link CompleteGoogleTechnicianRegistrationResource} into a
 * {@link CompleteGoogleTechnicianRegistrationCommand}.
 */
public class CompleteGoogleTechnicianRegistrationCommandFromResourceAssembler {

  /**
   * Converts the Google technician onboarding payload into its command representation.
   *
   * @param resource the {@link CompleteGoogleTechnicianRegistrationResource} resource to convert
   * @return the {@link CompleteGoogleTechnicianRegistrationCommand} command
   * @throws IllegalArgumentException when a required value is missing
   */
  public static CompleteGoogleTechnicianRegistrationCommand toCommandFromResource(
      CompleteGoogleTechnicianRegistrationResource resource) {
    var contactDetails = new ContactDetails(
        resource.phone(),
        resource.street(),
        resource.number(),
        resource.city(),
        resource.postalCode(),
        resource.country());
    return new CompleteGoogleTechnicianRegistrationCommand(
        resource.idToken(),
        contactDetails,
        resource.speciality(),
        resource.certificationNumber());
  }
}
