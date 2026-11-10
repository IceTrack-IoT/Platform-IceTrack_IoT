package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingResult;

/**
 * Outbound port through which the {@code assets} context turns an address into coordinates.
 *
 * <p>Implemented in infrastructure by {@code GoogleMapsGeocodingAdapter}. The lookup is synchronous
 * and bounded: implementations must return within a few seconds, and report every failure as a
 * {@link GeocodingResult} rather than throwing.</p>
 */
public interface GeocodingPort {

  /**
   * Looks up the coordinates of an address.
   *
   * @param address the free-form address to locate; required
   * @param placeId a place identifier chosen from the provider's suggestions, or {@code null} to
   *                locate the free-form address instead
   * @return the outcome of the lookup, carrying a location only when it resolved; never {@code null}
   */
  GeocodingResult geocode(String address, String placeId);
}
