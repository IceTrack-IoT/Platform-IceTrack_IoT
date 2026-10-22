package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.SitePersistenceEntity;

/**
 * Static assembler between the {@link Site} aggregate and its {@link SitePersistenceEntity}.
 *
 * <p>The only place that knows how the two shapes line up. Keeping the mapping out of both the
 * aggregate and the entity is what lets each of them change independently.</p>
 */
public final class SitePersistenceAssembler {

  private SitePersistenceAssembler() {
  }

  /**
   * Rebuilds the aggregate from a persisted row.
   *
   * @param entity the persisted row; may be {@code null}
   * @return the aggregate, or {@code null} when there is no row
   */
  public static Site toDomainFromPersistence(SitePersistenceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Site(
        entity.getId(),
        entity.getOwnerProfilesId(),
        entity.getVersion(),
        entity.getName(),
        entity.getAddress(),
        entity.getContactName(),
        entity.getPhone(),
        entity.getEquipmentCount(),
        entity.getGeocodingStatus(),
        toLocation(entity));
  }

  /**
   * Builds the persisted row of an aggregate.
   *
   * @param site the aggregate; may be {@code null}
   * @return the row to hand to Spring Data
   */
  public static SitePersistenceEntity toPersistenceFromDomain(Site site) {
    if (site == null) {
      return null;
    }
    var entity = new SitePersistenceEntity();
    entity.setId(site.getSiteId());
    entity.setVersion(site.getVersion());
    entity.setOwnerProfilesId(site.getOwnerId());
    entity.setName(site.getName());
    entity.setAddress(site.getAddress());
    entity.setContactName(site.getContactName());
    entity.setPhone(site.getPhone());
    entity.setEquipmentCount(site.getEquipmentCount());
    entity.setGeocodingStatus(site.getGeocodingStatus());
    var location = site.isLocated() ? site.getLocation() : null;
    entity.setLatitude(location == null ? null : location.latitude());
    entity.setLongitude(location == null ? null : location.longitude());
    entity.setPlaceId(location == null ? null : location.placeId());
    return entity;
  }

  /**
   * Rebuilds the location from its flattened columns.
   *
   * @param entity the persisted row
   * @return the location, or {@code null} when the row holds no coordinates
   */
  private static GeoLocation toLocation(SitePersistenceEntity entity) {
    if (entity.getLatitude() == null || entity.getLongitude() == null) {
      return null;
    }
    return new GeoLocation(entity.getLatitude(), entity.getLongitude(), entity.getPlaceId());
  }
}
