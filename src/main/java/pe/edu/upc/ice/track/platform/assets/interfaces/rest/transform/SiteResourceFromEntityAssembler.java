package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.GeoLocationResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.GeocodingStatusResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.SiteResource;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Assembler to convert a {@link Site} aggregate to a {@link SiteResource}.
 */
public class SiteResourceFromEntityAssembler {

  private static final String NAVIGATION_URL_BASE = "https://www.google.com/maps/dir/?api=1&destination=";

  /**
   * Converts a {@link Site} aggregate to a {@link SiteResource}.
   *
   * <p>The location and the navigation link are only filled in when the site is located; for any
   * other geocoding status both are {@code null}.</p>
   *
   * @param entity the {@link Site} aggregate to convert
   * @return the {@link SiteResource} resource
   */
  public static SiteResource toResourceFromEntity(Site entity) {
    var location = entity.isLocated() ? entity.getLocation() : null;
    return new SiteResource(
        entity.getSiteId(),
        entity.getOwnerId(),
        entity.getName(),
        entity.getAddress().value(),
        entity.getContactName(),
        entity.getPhone().value(),
        entity.getEquipmentCount(),
        GeocodingStatusResource.fromDomain(entity.getGeocodingStatus()),
        location == null ? null : new GeoLocationResource(location.latitude(), location.longitude(), location.placeId()),
        location == null ? null : toNavigationUrl(location));
  }

  /**
   * Builds a Google Maps directions link to a location.
   *
   * <p>Coordinates are written with {@link BigDecimal#toPlainString()}, which neither depends on
   * the server locale (no decimal comma) nor switches to scientific notation for small values.</p>
   *
   * @param location the resolved location
   * @return the directions link
   */
  private static String toNavigationUrl(GeoLocation location) {
    var url = NAVIGATION_URL_BASE
        + BigDecimal.valueOf(location.latitude()).toPlainString()
        + ","
        + BigDecimal.valueOf(location.longitude()).toPlainString();
    if (location.placeId() != null) {
      url += "&destination_place_id=" + URLEncoder.encode(location.placeId(), StandardCharsets.UTF_8);
    }
    return url;
  }
}
