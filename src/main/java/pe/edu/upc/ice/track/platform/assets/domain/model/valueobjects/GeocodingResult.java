package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.List;
import java.util.Objects;

/**
 * The answer of one geocoding lookup.
 *
 * <p>A location travels with {@link GeocodingOutcome#RESOLVED} and with nothing else, which is
 * enforced here so a caller can never read coordinates off a failed or ambiguous lookup.
 * Ambiguous lookups carry up to a few {@code candidates} so the owner can pick the right one.</p>
 *
 * @param outcome          what the lookup produced; required
 * @param location         the location found; required for {@code RESOLVED}, {@code null} otherwise
 * @param formattedAddress the normalized address Google returned; {@code null} unless resolved
 * @param district         the district Google named, or {@code null}
 * @param candidates       formatted addresses of the ambiguous matches; empty unless {@code NOT_FOUND}
 */
public record GeocodingResult(
    GeocodingOutcome outcome,
    GeoLocation location,
    String formattedAddress,
    String district,
    List<String> candidates) {

  /** Upper bound on the candidates reported to the client. */
  public static final int MAX_CANDIDATES = 5;

  /**
   * Validates that a location is present exactly when the lookup resolved.
   *
   * @throws IllegalArgumentException when the outcome and the location disagree
   */
  public GeocodingResult {
    Objects.requireNonNull(outcome, "outcome must not be null");
    candidates = candidates == null ? List.of() : List.copyOf(candidates);
    if (outcome == GeocodingOutcome.RESOLVED && location == null) {
      throw new IllegalArgumentException("A resolved geocoding result must carry a location");
    }
    if (outcome != GeocodingOutcome.RESOLVED && location != null) {
      throw new IllegalArgumentException("Only a resolved geocoding result may carry a location");
    }
  }

  public static GeocodingResult resolved(GeoLocation location, String formattedAddress, String district) {
    return new GeocodingResult(GeocodingOutcome.RESOLVED, location, formattedAddress, district, List.of());
  }

  public static GeocodingResult notFound(List<String> candidates) {
    return new GeocodingResult(GeocodingOutcome.NOT_FOUND, null, null, null, candidates);
  }

  public static GeocodingResult notFound() {
    return notFound(List.of());
  }

  public static GeocodingResult transientFailure() {
    return new GeocodingResult(GeocodingOutcome.TRANSIENT_FAILURE, null, null, null, List.of());
  }

  public static GeocodingResult permanentFailure() {
    return new GeocodingResult(GeocodingOutcome.PERMANENT_FAILURE, null, null, null, List.of());
  }
}
