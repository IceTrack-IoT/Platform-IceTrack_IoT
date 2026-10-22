package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Objects;

/**
 * GeoLocation Value Object.
 *
 * <p>The coordinates a {@code Site} address resolved to. Both coordinates are boxed and required,
 * so a location can never silently hold the {@code 0.0, 0.0} a missing primitive would default to:
 * a site that has no coordinates holds no {@code GeoLocation} at all.</p>
 *
 * @param latitude  degrees north, in {@code [-90, 90]}; required
 * @param longitude degrees east, in {@code [-180, 180]}; required
 * @param placeId   the provider's identifier for the place, or {@code null} when it gave none
 */
public record GeoLocation(Double latitude, Double longitude, String placeId) {

  /**
   * Validates the coordinate ranges and normalizes a blank place identifier to {@code null}.
   *
   * @throws IllegalArgumentException when a coordinate is missing, not finite or out of range
   */
  public GeoLocation {
    Objects.requireNonNull(latitude, "latitude must not be null");
    Objects.requireNonNull(longitude, "longitude must not be null");
    if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
      throw new IllegalArgumentException("Latitude must be within [-90, 90]: %s".formatted(latitude));
    }
    if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException("Longitude must be within [-180, 180]: %s".formatted(longitude));
    }
    placeId = placeId == null || placeId.isBlank() ? null : placeId.trim();
  }
}
