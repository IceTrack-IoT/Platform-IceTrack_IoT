package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * Why a site could not be located.
 *
 * <p>Recorded next to a {@link GeocodingStatus#FAILED} or {@link GeocodingStatus#NOT_FOUND} status,
 * and cleared as soon as the site is located or a new lookup starts. It lets a client tell the owner
 * what went wrong instead of only hiding the map link.</p>
 */
public enum GeocodingFailureReason {

  /** The provider answered, but matched no place or more than one. */
  ADDRESS_NOT_FOUND,

  /** The provider could not answer: it timed out, failed, or refused the request. */
  PROVIDER_UNAVAILABLE,

  /** The address resolved to a point outside Metropolitan Lima and Callao. */
  OUT_OF_COVERAGE,

  /** The lookup failed before reasons were recorded, so the cause is not known. */
  UNKNOWN
}
