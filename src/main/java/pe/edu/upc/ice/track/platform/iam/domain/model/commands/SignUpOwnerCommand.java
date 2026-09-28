package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;

/**
 * Sign up command registering an ice track owner with local credentials.
 *
 * <p>The role is implied by the command itself: the account is always created with
 * {@code OWNER_ROLE}, together with its {@code OwnerProfile}, in a single transaction.</p>
 *
 * @param username       the username of the account; required
 * @param password       the raw password of the account; required
 * @param email          the email of the account holder; required
 * @param fullName       the display name of the account holder; required
 * @param contactDetails the phone number and address captured by the onboarding form; required
 * @param ruc            the owner's taxpayer registration number; required. Its format is
 *                       enforced by the {@code profiles} context
 */
public record SignUpOwnerCommand(
    String username,
    String password,
    String email,
    String fullName,
    ContactDetails contactDetails,
    Long ruc) {

  /**
   * Validates that every component was supplied.
   */
  public SignUpOwnerCommand {
    requireText(username, "username");
    requireText(password, "password");
    requireText(email, "email");
    requireText(fullName, "fullName");
    if (contactDetails == null) {
      throw new IllegalArgumentException("contactDetails must not be null");
    }
    if (ruc == null) {
      throw new IllegalArgumentException("ruc must not be null");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be null or blank".formatted(field));
    }
  }
}
