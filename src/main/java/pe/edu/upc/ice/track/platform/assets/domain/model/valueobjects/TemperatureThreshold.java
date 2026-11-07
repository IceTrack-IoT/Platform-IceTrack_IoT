package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Objects;

/**
 * TemperatureThreshold Value Object.
 *
 * <p>The inclusive temperature band, in Celsius, a refrigeration unit must stay within. This
 * context owns it: it is defined and modified here, and the Monitoring and Alerting context only
 * reads it to decide whether a reading is an excursion.</p>
 *
 * <p>The platform monitors positive-temperature refrigeration only, measured by a DHT11 probe, so
 * both bounds must lie within {@value #LOWER_BOUND_CELSIUS} to {@value #UPPER_BOUND_CELSIUS} °C,
 * and {@code minCelsius} must be strictly lower than {@code maxCelsius}. A band violating either
 * rule is rejected at construction time, so no {@code Equipment} can ever carry an unusable
 * threshold. The same rules are re-asserted by a {@code CHECK} constraint on the
 * {@code equipments} table, because the database is the last line of defense against a value that
 * bypassed this class.</p>
 *
 * @param minCelsius the lowest acceptable temperature in Celsius; required, within the bounds and
 *                   strictly below {@code maxCelsius}
 * @param maxCelsius the highest acceptable temperature in Celsius; required, within the bounds and
 *                   strictly above {@code minCelsius}
 */
public record TemperatureThreshold(Double minCelsius, Double maxCelsius) {

  /** Lowest temperature a threshold may be set to: the platform only covers chilled storage. */
  public static final double LOWER_BOUND_CELSIUS = 0.0;

  /** Highest temperature a threshold may be set to. */
  public static final double UPPER_BOUND_CELSIUS = 30.0;

  /** Message returned when the band is inverted or degenerate. */
  public static final String ORDER_VIOLATION_MESSAGE = "minimum temperature must be lower than maximum";

  /**
   * Validates that the band is complete, within the refrigeration bounds and correctly ordered.
   *
   * @throws NullPointerException     when a bound is missing
   * @throws IllegalArgumentException when a bound is outside 0 to 30 °C, or {@code min} is not
   *                                  strictly lower than {@code max}
   */
  public TemperatureThreshold {
    Objects.requireNonNull(minCelsius, "minCelsius must not be null");
    Objects.requireNonNull(maxCelsius, "maxCelsius must not be null");
    requireWithinBounds("minCelsius", minCelsius);
    requireWithinBounds("maxCelsius", maxCelsius);
    if (minCelsius >= maxCelsius) {
      throw new IllegalArgumentException(ORDER_VIOLATION_MESSAGE);
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

  private static void requireWithinBounds(String field, double celsius) {
    if (!Double.isFinite(celsius) || celsius < LOWER_BOUND_CELSIUS || celsius > UPPER_BOUND_CELSIUS) {
      throw new IllegalArgumentException("%s must be between %s and %s °C, was %s"
          .formatted(field, LOWER_BOUND_CELSIUS, UPPER_BOUND_CELSIUS, celsius));
    }
  }
}
