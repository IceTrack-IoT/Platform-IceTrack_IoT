package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * Where a {@code Site} stands in turning its free-form address into coordinates.
 *
 * <p>Only {@link #RESOLVED} carries a location. Every other status means the site has no
 * coordinates to show, so a client never has to guess whether a pair of numbers is real.</p>
 */
public enum GeocodingStatus {

  /** The address was registered or changed and a lookup is queued or running. */
  PENDING,

  /** The address resolved to exactly one location, which the site now carries. */
  RESOLVED,

  /** The lookup could not be completed, either after exhausting its retries or because the
   * geocoding provider rejected the request. Submitting the address again, or restarting the
   * application, starts a new lookup. */
  FAILED,

  /** The provider answered, but with no location or with an ambiguous one. */
  NOT_FOUND
}
