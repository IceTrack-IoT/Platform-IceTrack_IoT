package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Objects;

/**
 * An address that Google Maps resolved to exactly one place inside the service coverage.
 *
 * <p>Produced by the address validation endpoint and carried to site registration inside a signed
 * token, so the coordinates saved with a site are the ones the owner verified.</p>
 *
 * @param formattedAddress the normalized address as Google writes it; required
 * @param placeId          Google's place identifier, or {@code null} when none was given
 * @param location         the resolved coordinates; required
 * @param district         the district name, or {@code null} when Google did not name one
 * @param area             the coverage area the location falls in; required
 */
public record VerifiedAddress(
    String formattedAddress,
    String placeId,
    GeoLocation location,
    String district,
    CoverageArea area) {

  public VerifiedAddress {
    Objects.requireNonNull(formattedAddress, "formattedAddress must not be null");
    Objects.requireNonNull(location, "location must not be null");
    Objects.requireNonNull(area, "area must not be null");
  }
}
