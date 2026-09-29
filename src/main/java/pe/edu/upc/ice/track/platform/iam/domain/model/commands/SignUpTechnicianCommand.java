package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;

/**
 * Sign up command registering an ice track maintenance technician with local credentials.
 *
 * <p>The role is implied by the command itself: the account is always created with
 * {@code TECHNICIAN_ROLE}, together with its {@code Technician} profile, in a single
 * transaction.</p>
 *
 * @param username            the username of the account; required
 * @param password            the raw password of the account; required
 * @param email               the email of the account holder; required
 * @param fullName            the display name of the account holder; required
 * @param contactDetails      the phone number and address captured by the onboarding form; required
 * @param speciality          the technician's speciality; required
 * @param certificationNumber the number of the technician's certification; required
 */
public record SignUpTechnicianCommand(
    String username,
    String password,
    String email,
    String fullName,
    ContactDetails contactDetails,
    String speciality,
    String certificationNumber) {

  /**
   * Validates that every component was supplied.
   */
  public SignUpTechnicianCommand {
    requireText(username, "username");
    requireText(password, "password");
    requireText(email, "email");
    requireText(fullName, "fullName");
    if (contactDetails == null) {
      throw new IllegalArgumentException("contactDetails must not be null");
    }
    requireText(speciality, "speciality");
    requireText(certificationNumber, "certificationNumber");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be null or blank".formatted(field));
    }
  }
}
