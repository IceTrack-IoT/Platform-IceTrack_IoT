package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import java.util.Locale;
import java.util.Set;

/**
 * Temperature Range Value Object.
 *
 * <p>The temperature window a dashboard filters on by default: its bounds, the unit they are
 * expressed in, and the label the clients display.</p>
 *
 * @param min   the lower bound, such as {@code -22}; required, never above {@code max}
 * @param max   the upper bound, such as {@code -18}; required, never below {@code min}
 * @param unit  the unit of both bounds, {@code "C"} (Celsius) or {@code "F"} (Fahrenheit); required
 * @param label the human-readable range, such as {@code "-18°C to -22°C"}; required
 */
public record TemperatureRange(Integer min, Integer max, String unit, String label) {

  /**
   * Units a temperature range can be expressed in.
   */
  public static final Set<String> SUPPORTED_UNITS = Set.of("C", "F");

  /**
   * Validates and normalizes the range.
   */
  public TemperatureRange {
    if (min == null) {
      throw new IllegalArgumentException("Temperature range min must not be null");
    }
    if (max == null) {
      throw new IllegalArgumentException("Temperature range max must not be null");
    }
    if (min > max) {
      throw new IllegalArgumentException("Temperature range min must not be greater than max");
    }
    if (unit == null || unit.isBlank()) {
      throw new IllegalArgumentException("Temperature range unit must not be null or blank");
    }
    unit = unit.trim().toUpperCase(Locale.ROOT);
    if (!SUPPORTED_UNITS.contains(unit)) {
      throw new IllegalArgumentException("Temperature range unit must be C or F");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("Temperature range label must not be null or blank");
    }
    label = label.trim();
  }
}
