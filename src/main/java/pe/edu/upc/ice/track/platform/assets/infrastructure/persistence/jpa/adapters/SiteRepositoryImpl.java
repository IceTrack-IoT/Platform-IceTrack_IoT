package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers.SitePersistenceAssembler;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories.SitePersistenceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the {@link SiteRepository} port with Spring Data JPA.
 *
 * <p>Also acts as the event-publishing boundary: a brand-new {@link Site} is only asked to raise
 * its {@code SiteCreatedEvent} <em>after</em> the insert, because the event carries the
 * persistence identity and there is nothing to announce before it exists. Publishing here, rather
 * than from the command service, is what guarantees that identity is present and that the event
 * follows the same fate as the transaction that produced the row.</p>
 */
@Repository
public class SiteRepositoryImpl implements SiteRepository {

  private final SitePersistenceRepository sitePersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor
   *
   * @param sitePersistenceRepository the Spring Data repository
   * @param eventPublisher            the Spring event publisher used to dispatch domain events
   */
  public SiteRepositoryImpl(
      SitePersistenceRepository sitePersistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.sitePersistenceRepository = sitePersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  // inherited javadoc
  @Override
  public Optional<Site> findById(Long siteId) {
    if (siteId == null) {
      return Optional.empty();
    }
    return sitePersistenceRepository.findById(siteId)
        .map(SitePersistenceAssembler::toDomainFromPersistence);
  }

  // inherited javadoc
  @Override
  public List<Site> findByOwnerId(Long ownerId) {
    if (ownerId == null) {
      return List.of();
    }
    return sitePersistenceRepository.findByOwnerProfilesIdOrderByNameAsc(ownerId).stream()
        .map(SitePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  // inherited javadoc
  @Override
  public boolean existsById(Long siteId) {
    return siteId != null && sitePersistenceRepository.existsById(siteId);
  }

  /**
   * Persists a site and, when it was new, announces it.
   *
   * @param site the site to persist
   * @return the persisted site
   */
  @Override
  public Site save(Site site) {
    var isNew = site.getSiteId() == null;
    var savedEntity = sitePersistenceRepository.save(
        SitePersistenceAssembler.toPersistenceFromDomain(site));
    var savedSite = SitePersistenceAssembler.toDomainFromPersistence(savedEntity);
    if (isNew) {
      savedSite.onCreated();
      savedSite.domainEvents().forEach(eventPublisher::publishEvent);
      savedSite.clearDomainEvents();
    }
    return savedSite;
  }
}