package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * Value Object representing a temperature measurement in Celsius.
 *
 * <p>Only rejects values no refrigeration context could produce. The narrower physical range of
 * the probe is enforced on raw readings by {@code SensorReading}; a calibrated value may lie up
 * to the maximum calibration offset beyond it, so this object accepts up to
 * {@value #MAX_PLAUSIBLE_CELSIUS} °C.</p>
 *
 * @param celsius the measured value, or {@code null} when not applicable
 */
public record Temperature(Double celsius) {

  private static final double MIN_PLAUSIBLE_CELSIUS = -50.0;
  private static final double MAX_PLAUSIBLE_CELSIUS = 60.0;

  public Temperature {
    if (celsius != null && (celsius < MIN_PLAUSIBLE_CELSIUS || celsius > MAX_PLAUSIBLE_CELSIUS)) {
      throw new IllegalArgumentException(
          "Temperature out of plausible range for a refrigeration unit: " + celsius + "C");
    }
  }

  public boolean isAbove(Temperature other) {
    return celsius != null && other.celsius != null && celsius > other.celsius;
  }

  public boolean isBelow(Temperature other) {
    return celsius != null && other.celsius != null && celsius < other.celsius;
  }
}
