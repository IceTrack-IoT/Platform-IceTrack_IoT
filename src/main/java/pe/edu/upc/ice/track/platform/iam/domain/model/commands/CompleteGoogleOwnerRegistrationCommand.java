package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;

/**
 * Completes the deferred registration of a Google account as an ice track owner.
 *
 * <p>Second step of the Google flow, once a {@link SignInByGoogleCommand} has reported that the
 * Google account is unknown. The id_token is verified again - it is the only trusted source of the
 * email, subject and name - and the account is created with {@code OWNER_ROLE} together with its
 * {@code Owner} profile in a single transaction.</p>
 *
 * @param idToken        the Google OIDC id_token issued to the frontend; required
 * @param username       the desired username, unique on the platform; required. The Google
 *                       email is never used as username; it is stored on the owner profile
 * @param contactDetails the phone number and address captured by the onboarding form; required
 * @param ruc            the owner's taxpayer registration number; required
 */
public record CompleteGoogleOwnerRegistrationCommand(
    String idToken,
    String username,
    ContactDetails contactDetails,
    Long ruc) {

  /**
   * Validates that every component was supplied.
   */
  public CompleteGoogleOwnerRegistrationCommand {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException("Google id_token must not be null or blank");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username must not be null or blank");
    }
    if (contactDetails == null) {
      throw new IllegalArgumentException("contactDetails must not be null");
    }
    if (ruc == null) {
      throw new IllegalArgumentException("ruc must not be null");
    }
  }
}
