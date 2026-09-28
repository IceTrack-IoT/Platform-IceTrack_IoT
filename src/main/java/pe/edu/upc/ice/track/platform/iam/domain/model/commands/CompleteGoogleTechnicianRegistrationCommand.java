package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;

/**
 * Completes the deferred registration of a Google account as an ice track maintenance technician.
 *
 * <p>Second step of the Google flow, once a {@link SignInByGoogleCommand} has reported that the
 * Google account is unknown. The id_token is verified again - it is the only trusted source of the
 * email, subject and name - and the account is created with {@code TECHNICIAN_ROLE} together with
 * its {@code TechnicianProfile} in a single transaction.</p>
 *
 * @param idToken             the Google OIDC id_token issued to the frontend; required
 * @param contactDetails      the phone number and address captured by the onboarding form; required
 * @param speciality          the technician's speciality; required
 * @param certificationNumber the number of the technician's certification; required
 */
public record CompleteGoogleTechnicianRegistrationCommand(
    String idToken,
    ContactDetails contactDetails,
    String speciality,
    String certificationNumber) {

  /**
   * Validates that every component was supplied.
   */
  public CompleteGoogleTechnicianRegistrationCommand {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException("Google id_token must not be null or blank");
    }
    if (contactDetails == null) {
      throw new IllegalArgumentException("contactDetails must not be null");
    }
    if (speciality == null || speciality.isBlank()) {
      throw new IllegalArgumentException("speciality must not be null or blank");
    }
    if (certificationNumber == null || certificationNumber.isBlank()) {
      throw new IllegalArgumentException("certificationNumber must not be null or blank");
    }
  }
}
