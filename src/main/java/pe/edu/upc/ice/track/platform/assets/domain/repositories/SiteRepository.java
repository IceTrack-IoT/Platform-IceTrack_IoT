package pe.edu.upc.ice.track.platform.assets.domain.repositories;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;

import java.util.List;
import java.util.Optional;

/**
 * Site repository port.
 *
 * <p>Implemented by {@code SiteRepositoryImpl} in the infrastructure layer, which is also the
 * place where the {@code SiteCreatedEvent} of a brand new site is published.</p>
 */
public interface SiteRepository {

  /**
   * Find a site by its identifier, whatever its owner.
   *
   * <p>Ownership is applied by the query service, not here, so that a site belonging to somebody
   * else and a site that does not exist stay indistinguishable to the caller.</p>
   *
   * @param siteId the site identifier
   * @return the site, or empty when no site carries that identifier
   */
  Optional<Site> findById(Long siteId);

  /**
   * Find every site belonging to an owner.
   *
   * @param ownerId the owner identifier
   * @return the sites of that owner, empty when it has none
   */
  List<Site> findByOwnerId(Long ownerId);

  /**
   * Check whether a site already exists.
   *
   * @param siteId the site identifier
   * @return {@code true} when a site carries that identifier
   */
  boolean existsById(Long siteId);

  /**
   * Persist a site, inserting it when it is new and updating it otherwise.
   *
   * @param site the site to persist
   * @return the persisted site, carrying its identity and having published its creation event
   *         when it was new
   */
  Site save(Site site);
}