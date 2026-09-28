package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * Ruc Value Object.
 *
 * <p>The Peruvian taxpayer registration number (Registro Único de Contribuyentes) of an ice track
 * owner. It is always an 11 digit number.</p>
 *
 * @param value the registration number; required and exactly 11 digits long
 */
public record Ruc(Long value) {

  private static final long MIN_VALUE = 10_000_000_000L;
  private static final long MAX_VALUE = 99_999_999_999L;

  /**
   * Validates that the registration number is present and 11 digits long.
   */
  public Ruc {
    if (value == null) {
      throw new IllegalArgumentException("RUC must not be null");
    }
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new IllegalArgumentException("RUC must be an 11 digit number");
    }
  }
}
