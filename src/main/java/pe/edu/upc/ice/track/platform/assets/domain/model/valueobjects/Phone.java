package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * Phone Value Object.
 *
 * <p>Contact number of the person reachable at a {@code Site}. Deliberately narrower than
 * {@code profiles.domain.model.valueobjects.Phone}: this context never models a national number
 * separately from its dialling prefix, it only needs a number that can be dialled.</p>
 *
 * <p>Accepted shape is digits and spaces with at most one leading {@code +} for the international
 * prefix, so {@code "+51 987654321"}, {@code "987654321"} and {@code "+51 987 654 321"} are all
 * valid while letters, parentheses and stray symbols are not. The digit count is bounded by
 * {@link #MIN_DIGITS} and {@link #MAX_DIGITS}, the range E.164 allows.</p>
 *
 * @param value the phone number; required, {@code +} optional, 7 to 15 digits
 */
public record Phone(String value) {

  /**
   * Minimum number of digits a diallable national or international number may hold.
   */
  public static final int MIN_DIGITS = 7;

  /**
   * Maximum number of digits a diallable number may hold, as allowed by E.164.
   */
  public static final int MAX_DIGITS = 15;

  /**
   * Validates the number format and normalises it by collapsing the runs of spaces a human types
   * into single spaces.
   *
   * @throws IllegalArgumentException when the number is missing, malformed or of implausible length
   */
  public Phone {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Phone must not be null or blank");
    }
    var candidate = value.trim().replaceAll("\\s+", " ");
    if (!candidate.matches("\\+?\\d+( \\d+)*")) {
      throw new IllegalArgumentException(
          "Phone must contain digits only, optionally prefixed by '+' and separated by single spaces");
    }
    var digits = candidate.chars().filter(Character::isDigit).count();
    if (digits < MIN_DIGITS) {
      throw new IllegalArgumentException("Phone must contain at least %d digits".formatted(MIN_DIGITS));
    }
    if (digits > MAX_DIGITS) {
      throw new IllegalArgumentException("Phone must not exceed %d digits".formatted(MAX_DIGITS));
    }
    value = candidate;
  }
}