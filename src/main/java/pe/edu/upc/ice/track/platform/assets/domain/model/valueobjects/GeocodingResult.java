package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Objects;

/**
 * GeocodingResult Value Object.
 *
 * <p>The answer of one geocoding lookup. A location travels with {@link GeocodingOutcome#RESOLVED}
 * and with nothing else, and that pairing is enforced here so that a caller can never read
 * coordinates off a failed or empty lookup.</p>
 *
 * @param outcome  what the lookup produced; required
 * @param location the location found; required for {@code RESOLVED}, {@code null} otherwise
 */
public record GeocodingResult(GeocodingOutcome outcome, GeoLocation location) {

  /**
   * Validates that a location is present exactly when the lookup resolved.
   *
   * @throws IllegalArgumentException when the outcome and the location disagree
   */
  public GeocodingResult {
    Objects.requireNonNull(outcome, "outcome must not be null");
    if (outcome == GeocodingOutcome.RESOLVED && location == null) {
      throw new IllegalArgumentException("A resolved geocoding result must carry a location");
    }
    if (outcome != GeocodingOutcome.RESOLVED && location != null) {
      throw new IllegalArgumentException("Only a resolved geocoding result may carry a location");
    }
  }

  public static GeocodingResult resolved(GeoLocation location) {
    return new GeocodingResult(GeocodingOutcome.RESOLVED, location);
  }

  public static GeocodingResult notFound() {
    return new GeocodingResult(GeocodingOutcome.NOT_FOUND, null);
  }

  public static GeocodingResult transientFailure() {
    return new GeocodingResult(GeocodingOutcome.TRANSIENT_FAILURE, null);
  }

  public static GeocodingResult permanentFailure() {
    return new GeocodingResult(GeocodingOutcome.PERMANENT_FAILURE, null);
  }
}
