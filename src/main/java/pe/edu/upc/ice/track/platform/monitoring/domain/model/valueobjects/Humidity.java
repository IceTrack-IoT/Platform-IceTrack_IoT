package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * Value Object representing a relative humidity measurement, as a percentage.
 * @param percentage the measured value between 0 and 100, or {@code null} when not reported
 */
public record Humidity(Double percentage) {

  public Humidity {
    if (percentage != null && (percentage < 0.0 || percentage > 100.0)) {
      throw new IllegalArgumentException("Humidity must be a percentage between 0 and 100: " + percentage);
    }
  }
}
