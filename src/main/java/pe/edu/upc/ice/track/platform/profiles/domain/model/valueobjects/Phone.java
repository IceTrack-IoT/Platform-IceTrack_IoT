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

  /**
   * Builds a phone number out of a single raw value, as supplied by a form or another bounded
   * context that does not model the country code separately.
   *
   * <p>A leading {@code +NN} prefix followed by a space is split off as the country code;
   * otherwise the whole value is kept as the national number with an unknown country code.</p>
   *
   * @param rawPhone the raw phone number, such as {@code "+51 987654321"}; required
   * @return the phone number
   * @throws IllegalArgumentException when the phone number is missing
   */
  public static Phone fromString(String rawPhone) {
    if (rawPhone == null || rawPhone.isBlank()) {
      throw new IllegalArgumentException("Phone number must not be null or blank");
    }
    var trimmed = rawPhone.trim();
    var separatorIndex = trimmed.indexOf(' ');
    if (trimmed.startsWith("+") && separatorIndex > 1 && separatorIndex < trimmed.length() - 1) {
      return new Phone(trimmed.substring(0, separatorIndex), trimmed.substring(separatorIndex + 1).trim());
    }
    return withoutCountryCode(trimmed);
  }

  public String getFullNumber() {
    return "%s %s".formatted(countryCode, phoneNumber).trim();
  }
}
