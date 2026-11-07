package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

/**
 * Raised by a strategy when a unit recorded no telemetry in the requested range, so the metric
 * cannot be computed. The report is marked {@code FAILED} with this message as its reason.
 */
public class InsufficientTelemetryException extends RuntimeException {

  /** The reason shown to the owner. */
  public static final String REASON = "Insufficient telemetry data for the period";

  public InsufficientTelemetryException() {
    super(REASON);
  }
}
