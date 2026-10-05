package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * Address Value Object.
 *
 * <p>The physical location of a {@code Site}. Kept as a single free-form line, at most
 * {@value #MAX_LENGTH} characters, rather than as the decomposed street/city/postal-code
 * structure a profile address uses: a refrigeration site is located by whatever the owner typed
 * in, and splitting it would force the domain to reject perfectly valid Peruvian addresses that
 * do not fit a rigid structure.</p>
 *
 * <p>Immutable and self-validating: an address that is blank or too long can never be
 * constructed, so no {@code Site} can ever hold one.</p>
 *
 * @param value the address line; required, not blank, at most 50 characters
 */
public record Address(String value) {

  /**
   * Maximum number of characters an address may hold, matching the {@code address} column width.
   */
  public static final int MAX_LENGTH = 50;

  /**
   * Validates and trims the address.
   *
   * @throws IllegalArgumentException when the address is missing, blank or too long
   */
  public Address {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Address must not be null or blank");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("Address must not exceed %d characters".formatted(MAX_LENGTH));
    }
  }
}