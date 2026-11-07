package pe.edu.upc.ice.track.platform.assets.domain.model.policies;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CoverageArea;

import java.util.Optional;

/**
 * Decides whether a point lies inside the service coverage: Metropolitan Lima or the Constitutional
 * Province of Callao.
 *
 * <p>The areas are approximated by bounding boxes around the two jurisdictions. They are wide enough
 * to include every district of both areas and narrow enough to exclude the neighbouring provinces
 * to the north and south. Callao is tested first because its box lies inside Lima's.</p>
 */
public final class CoveragePolicy {

  private static final double CALLAO_MIN_LAT = -12.07;
  private static final double CALLAO_MAX_LAT = -11.77;
  private static final double CALLAO_MIN_LNG = -77.20;
  private static final double CALLAO_MAX_LNG = -77.05;

  private static final double LIMA_MIN_LAT = -12.65;
  private static final double LIMA_MAX_LAT = -11.65;
  private static final double LIMA_MIN_LNG = -77.25;
  private static final double LIMA_MAX_LNG = -76.65;

  private CoveragePolicy() {
  }

  /**
   * Finds the coverage area of a point.
   *
   * @param latitude  latitude in degrees
   * @param longitude longitude in degrees
   * @return the area the point is in, or empty when it is outside both
   */
  public static Optional<CoverageArea> areaOf(double latitude, double longitude) {
    if (within(latitude, longitude, CALLAO_MIN_LAT, CALLAO_MAX_LAT, CALLAO_MIN_LNG, CALLAO_MAX_LNG)) {
      return Optional.of(CoverageArea.CALLAO);
    }
    if (within(latitude, longitude, LIMA_MIN_LAT, LIMA_MAX_LAT, LIMA_MIN_LNG, LIMA_MAX_LNG)) {
      return Optional.of(CoverageArea.LIMA_METROPOLITANA);
    }
    return Optional.empty();
  }

  private static boolean within(double lat, double lng, double minLat, double maxLat, double minLng, double maxLng) {
    return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
  }
}
