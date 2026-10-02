package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
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
        entity.getName(),
        entity.getAddress(),
        entity.getContactName(),
        entity.getPhone());
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
    entity.setOwnerProfilesId(site.getOwnerId());
    entity.setName(site.getName());
    entity.setAddress(site.getAddress());
    entity.setContactName(site.getContactName());
    entity.setPhone(site.getPhone());
    return entity;
  }
}