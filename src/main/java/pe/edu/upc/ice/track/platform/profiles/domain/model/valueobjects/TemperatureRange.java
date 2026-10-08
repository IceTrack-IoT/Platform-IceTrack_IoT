package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * Temperature Range Value Object.
 *
 * <p>The temperature window a dashboard filters on by default, kept as the machine-readable value
 * the clients exchange together with the label they display.</p>
 *
 * @param value the range value, such as {@code "-18_-22"}; required
 * @param label the human-readable range, such as {@code "-18 °C to -22 °C"}; required
 */
public record TemperatureRange(String value, String label) {

  /**
   * Validates and normalizes the range.
   */
  public TemperatureRange {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Temperature range value must not be null or blank");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("Temperature range label must not be null or blank");
    }
    value = value.trim();
    label = label.trim();
  }
}
