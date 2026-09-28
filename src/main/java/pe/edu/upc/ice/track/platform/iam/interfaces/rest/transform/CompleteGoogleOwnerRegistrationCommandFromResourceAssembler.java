package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleOwnerRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CompleteGoogleOwnerRegistrationResource;

/**
 * Assembler that converts a {@link CompleteGoogleOwnerRegistrationResource} into a
 * {@link CompleteGoogleOwnerRegistrationCommand}.
 */
public class CompleteGoogleOwnerRegistrationCommandFromResourceAssembler {

  /**
   * Converts the Google owner onboarding payload into its command representation.
   *
   * @param resource the {@link CompleteGoogleOwnerRegistrationResource} resource to convert
   * @return the {@link CompleteGoogleOwnerRegistrationCommand} command
   * @throws IllegalArgumentException when a required value is missing
   */
  public static CompleteGoogleOwnerRegistrationCommand toCommandFromResource(CompleteGoogleOwnerRegistrationResource resource) {
    var contactDetails = new ContactDetails(
        resource.phone(),
        resource.street(),
        resource.number(),
        resource.city(),
        resource.postalCode(),
        resource.country());
    return new CompleteGoogleOwnerRegistrationCommand(resource.idToken(), contactDetails, resource.ruc());
  }
}
