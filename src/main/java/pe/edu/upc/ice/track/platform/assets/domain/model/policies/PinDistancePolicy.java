package pe.edu.upc.ice.track.platform.assets.domain.model.policies;

/**
 * Limits how far an owner may move the pin away from the verified coordinates of an address.
 *
 * <p>The verified point comes from Google Maps; the owner may refine it to the site entrance, but
 * not to a different building. Distances use the Haversine formula over a spherical Earth.</p>
 */
public final class PinDistancePolicy {

  /** Largest allowed distance between the verified point and the pin, in meters. */
  public static final double MAX_METERS = 150.0;

  private static final double EARTH_RADIUS_METERS = 6_371_000.0;

  private PinDistancePolicy() {
  }

  /**
   * Tells whether two points are within the allowed pin distance.
   *
   * @param lat1 latitude of the verified point, in degrees
   * @param lng1 longitude of the verified point, in degrees
   * @param lat2 latitude of the pin, in degrees
   * @param lng2 longitude of the pin, in degrees
   * @return {@code true} when the distance is at most {@link #MAX_METERS}
   */
  public static boolean withinRange(double lat1, double lng1, double lat2, double lng2) {
    var dLat = Math.toRadians(lat2 - lat1);
    var dLng = Math.toRadians(lng2 - lng1);
    var a = Math.pow(Math.sin(dLat / 2), 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
    return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a))) <= MAX_METERS;
  }
}
