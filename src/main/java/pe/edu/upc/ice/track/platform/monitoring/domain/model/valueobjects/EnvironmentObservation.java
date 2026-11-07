package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 * Value Object grouping what the light and PIR sensors observed during one reading interval.
 *
 * <p>Every component is optional, because a board may carry neither sensor or one may stop
 * responding. {@code null} always means "not reported": a missing presence flag is never read as
 * "nobody was there", and a missing light level is never read as a closed door. Presence is a bare
 * flag; nothing that could identify a person is kept.</p>
 *
 * @param lightLevelMax    highest light level seen inside the unit, 0 to 100 %, or {@code null}
 * @param doorOpenSeconds  seconds the door was detected open during the interval, zero or more,
 *                         or {@code null}
 * @param presenceDetected whether the PIR sensor detected motion near the unit, or {@code null}
 *                         when presence is unavailable
 */
public record EnvironmentObservation(Double lightLevelMax, Integer doorOpenSeconds, Boolean presenceDetected) {

  public static final double MIN_LIGHT_LEVEL = 0.0;
  public static final double MAX_LIGHT_LEVEL = 100.0;

  /** An interval for which neither sensor reported anything. */
  public static final EnvironmentObservation UNAVAILABLE = new EnvironmentObservation(null, null, null);

  public EnvironmentObservation {
    if (lightLevelMax != null && (!Double.isFinite(lightLevelMax)
        || lightLevelMax < MIN_LIGHT_LEVEL || lightLevelMax > MAX_LIGHT_LEVEL)) {
      throw new IllegalArgumentException("lightLevelMax must be between %s and %s %%, was %s"
          .formatted(MIN_LIGHT_LEVEL, MAX_LIGHT_LEVEL, lightLevelMax));
    }
    if (doorOpenSeconds != null && doorOpenSeconds < 0) {
      throw new IllegalArgumentException("doorOpenSeconds must not be negative, was " + doorOpenSeconds);
    }
  }
}
