package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST representation of where a site stands in locating its address.
 *
 * <p>A separate type from the domain {@code GeocodingStatus} for the same reason as
 * {@link EquipmentTypeResource}: the published API keeps a vocabulary of its own.</p>
 */
@Schema(
    name = "GeocodingStatus",
    description = "Where the site stands in locating its address: PENDING while a lookup is queued or "
        + "running, RESOLVED once a location is known, NOT_FOUND when the address matched no single "
        + "place, FAILED when the lookup could not be completed",
    example = "RESOLVED")
public enum GeocodingStatusResource {

  PENDING,
  RESOLVED,
  FAILED,
  NOT_FOUND;

  /**
   * Translates a domain geocoding status into this REST constant.
   *
   * @param status the domain geocoding status; required
   * @return the matching REST constant
   */
  public static GeocodingStatusResource fromDomain(
      pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus status) {
    return valueOf(status.name());
  }
}
