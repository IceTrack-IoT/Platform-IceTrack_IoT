package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

/**
 * Contact details captured by the onboarding form.
 *
 * <p>IAM does not own these values: it only carries them from the registration request to the
 * {@code profiles} context, which turns them into its own value objects and enforces their format.
 * IAM merely guarantees that the mandatory ones were supplied, so an incomplete form is rejected
 * before any account is written.</p>
 *
 * @param phone      phone number; required
 * @param street     street of the address; required
 * @param number     street number or apartment; may be {@code null}
 * @param city       city of the address; required
 * @param postalCode postal code of the address; required
 * @param country    country of the address; required
 */
public record ContactDetails(
    String phone,
    String street,
    String number,
    String city,
    String postalCode,
    String country) {

  /**
   * Validates that the mandatory components were supplied.
   */
  public ContactDetails {
    requireText(phone, "phone");
    requireText(street, "street");
    requireText(city, "city");
    requireText(postalCode, "postalCode");
    requireText(country, "country");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be null or blank".formatted(field));
    }
  }
}
