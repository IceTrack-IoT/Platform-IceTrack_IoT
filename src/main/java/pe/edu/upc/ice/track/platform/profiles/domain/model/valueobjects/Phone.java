package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * Phone Value Object.
 *
 * <p>The country code is optional: other bounded contexts and identity providers hand over a
 * phone number as a single opaque string, without modelling the dialling prefix. The national
 * number is always required.</p>
 *
 * @param countryCode the country code of the phone number, may be {@code null}
 * @param phoneNumber the phone number
 */
public record Phone(String countryCode, String phoneNumber) {

  public Phone {
    if (phoneNumber == null || phoneNumber.isBlank()) {
      throw new IllegalArgumentException("Phone number must not be null or blank");
    }
    countryCode = countryCode == null ? "" : countryCode.trim();
    phoneNumber = phoneNumber.trim();
  }

  /**
   * Builds a phone number whose country code is unknown.
   *
   * @param phoneNumber the national phone number
   * @return the phone number without a dialling prefix
   */
  public static Phone withoutCountryCode(String phoneNumber) {
    return new Phone(null, phoneNumber);
  }

  public String getFullNumber() {
    return "%s %s".formatted(countryCode, phoneNumber).trim();
  }
}
