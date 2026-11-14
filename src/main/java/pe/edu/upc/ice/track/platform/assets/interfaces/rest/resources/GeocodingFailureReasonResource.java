package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingFailureReason;

/**
 * REST representation of why a site could not be located.
 *
 * <p>A separate type from the domain {@code GeocodingFailureReason} for the same reason as
 * {@link GeocodingStatusResource}: the published API keeps a vocabulary of its own.</p>
 */
@Schema(
    name = "GeocodingFailureReason",
    description = "Why the site has no location: ADDRESS_NOT_FOUND when the maps service matched no single "
        + "place, PROVIDER_UNAVAILABLE when it could not answer, OUT_OF_COVERAGE when the address lies outside "
        + "Metropolitan Lima and Callao, UNKNOWN when the cause was not recorded",
    example = "ADDRESS_NOT_FOUND")
public enum GeocodingFailureReasonResource {

  ADDRESS_NOT_FOUND,
  PROVIDER_UNAVAILABLE,
  OUT_OF_COVERAGE,
  UNKNOWN;

  /**
   * Translates a domain failure reason into this REST constant.
   *
   * @param reason the domain failure reason; may be {@code null}
   * @return the matching REST constant, or {@code null} when there is no reason
   */
  public static GeocodingFailureReasonResource fromDomain(GeocodingFailureReason reason) {
    return reason == null ? null : valueOf(reason.name());
  }
}
