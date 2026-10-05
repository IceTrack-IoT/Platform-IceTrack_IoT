package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Objects;

/**
 * TemperatureThreshold Value Object.
 *
 * <p>The inclusive temperature band, in Celsius, a refrigeration unit must stay within. This
 * context owns it: it is defined and modified here, and the Monitoring and Alerting context only
 * reads it to decide whether a reading is an excursion.</p>
 *
 * <p>The single invariant of this value object is that {@code minCelsius} is strictly lower than
 * {@code maxCelsius}. An inverted or degenerate band - a unit that would have to be simultaneously
 * below and above the same temperature - is rejected at construction time, so no
 * {@code Equipment} can ever carry an unusable threshold. The same rule is re-asserted by a
 * {@code CHECK} constraint on the {@code assets.equipments} table, because the database is the
 * last line of defence against a value that bypassed this class.</p>
 *
 * @param minCelsius the lowest acceptable temperature in Celsius; required and strictly below
 *                   {@code maxCelsius}
 * @param maxCelsius the highest acceptable temperature in Celsius; required and strictly above
 *                   {@code minCelsius}
 */
public record TemperatureThreshold(Double minCelsius, Double maxCelsius) {

  /**
   * Coldest temperature a refrigeration unit may plausibly have to be rated for. Anything below
   * this is a sensor fault rather than a refrigeration condition.
   */
  private static final double MIN_PLAUSIBLE_CELSIUS = -80.0;

  /**
   * Warmest temperature a refrigeration unit may plausibly have to be rated for.
   */
  private static final double MAX_PLAUSIBLE_CELSIUS = 50.0;

  /**
   * Validates that the band is complete, physically plausible and correctly ordered.
   *
   * @throws IllegalArgumentException when a bound is missing, implausible, or {@code min} is not
   *                                  strictly lower than {@code max}
   */
  public TemperatureThreshold {
    Objects.requireNonNull(minCelsius, "minCelsius must not be null");
    Objects.requireNonNull(maxCelsius, "maxCelsius must not be null");
    if (minCelsius < MIN_PLAUSIBLE_CELSIUS || maxCelsius > MAX_PLAUSIBLE_CELSIUS) {
      throw new IllegalArgumentException(
          "Temperature threshold out of plausible range for a refrigeration unit: [%s, %s]C"
              .formatted(minCelsius, maxCelsius));
    }
    if (minCelsius >= maxCelsius) {
      throw new IllegalArgumentException(
          "Temperature threshold minimum must be strictly lower than its maximum: [%s, %s]C"
              .formatted(minCelsius, maxCelsius));
    }
  }

  /**
   * Tells whether a measured temperature falls inside this band, bounds included.
   *
   * @param celsius the measured temperature
   * @return {@code true} when the reading is within the accepted range
   */
  public boolean accepts(Double celsius) {
    return celsius != null && celsius >= minCelsius && celsius <= maxCelsius;
  }
}