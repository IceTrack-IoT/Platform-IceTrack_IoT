package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingResult;

/**
 * Outbound port through which the {@code assets} context turns an address into coordinates.
 *
 * <p>Owned by the application layer and implemented in infrastructure by
 * {@code GoogleMapsGeocodingAdapter}, so the dependency points inwards: the geocoding lifecycle
 * knows this interface only, and the provider can be swapped - or stubbed in a test - without
 * touching it.</p>
 *
 * <p>Implementations report every failure as a {@link GeocodingResult} rather than throwing, and
 * must bound how long a single call may block: the caller retries on its own schedule and has to
 * be able to tell a failure worth retrying from one that is not.</p>
 */
public interface GeocodingPort {

  /**
   * Looks up the coordinates of an address.
   *
   * @param address the free-form address to locate
   * @return the outcome of the lookup, carrying a location only when it resolved; never
   *         {@code null}
   */
  GeocodingResult geocode(String address);
}
