package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
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
 *
 * <p>Events raised by an <em>update</em> - a {@code SiteAddressChangedEvent} - are dispatched here
 * too, right after the row is written, exactly as {@code EquipmentRepositoryImpl} does for its
 * own updates.</p>
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
  public List<Site> findByGeocodingStatus(GeocodingStatus geocodingStatus) {
    if (geocodingStatus == null) {
      return List.of();
    }
    return sitePersistenceRepository.findByGeocodingStatus(geocodingStatus).stream()
        .map(SitePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  // inherited javadoc
  @Override
  public boolean existsById(Long siteId) {
    return siteId != null && sitePersistenceRepository.existsById(siteId);
  }

  /**
   * Persists a site and dispatches whatever domain events the change raised.
   *
   * <p>The events an update raised are read from the incoming aggregate, which is the one the
   * caller changed; the creation events are raised on the rehydrated copy, because they carry the
   * identity the incoming aggregate does not have yet.</p>
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
    publishPendingEvents(site);
    if (isNew) {
      savedSite.onCreated();
      publishPendingEvents(savedSite);
    }
    return savedSite;
  }

  /**
   * Dispatches the events the aggregate registered, then clears them so a later save of the same
   * aggregate does not announce the same change twice.
   *
   * @param site the aggregate that was just persisted
   */
  private void publishPendingEvents(Site site) {
    site.domainEvents().forEach(eventPublisher::publishEvent);
    site.clearDomainEvents();
  }
}
