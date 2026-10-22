package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * What a single geocoding lookup produced.
 *
 * <p>Distinct from {@link GeocodingStatus}: an outcome describes one attempt, while the status is
 * what the site ends up recording once retries are over. The split between the two failure kinds
 * is what decides whether an attempt is worth repeating.</p>
 */
public enum GeocodingOutcome {

  /** Exactly one unambiguous location was found. */
  RESOLVED,

  /** The provider answered and found nothing usable: no result, or more than one candidate. */
  NOT_FOUND,

  /** The attempt failed for a reason that may clear up on its own - a timeout, a 5xx, a rate
   * limit - so it is worth retrying. */
  TRANSIENT_FAILURE,

  /** The provider rejected the request itself - an invalid key, a disabled API, an exhausted
   * daily quota - so retrying within seconds cannot succeed. */
  PERMANENT_FAILURE
}
