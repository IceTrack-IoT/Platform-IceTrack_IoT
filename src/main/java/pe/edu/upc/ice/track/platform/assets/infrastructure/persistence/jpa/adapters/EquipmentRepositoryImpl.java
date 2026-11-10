package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.adapters;

import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers.EquipmentPersistenceAssembler;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.SitePersistenceEntity;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories.EquipmentPersistenceRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories.SitePersistenceRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.specifications.EquipmentPersistenceSpecifications;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository adapter that bridges the {@link EquipmentRepository} port with Spring Data JPA.
 *
 * <p>Like its site counterpart, this is also where a brand-new unit raises its
 * {@code EquipmentRegisteredEvent}: the event carries the persistence identity, so it can only be
 * assembled once the insert has happened.</p>
 *
 * <p>Events raised by an <em>update</em> - currently only {@code TemperatureThresholdUpdatedEvent}
 * - are dispatched here too, right after the row is written, so that a change published to the rest
 * of the platform is always one that reached the database.</p>
 *
 * <p><strong>Site name.</strong> A unit is joined with its site by identifier, since
 * {@code EquipmentPersistenceEntity} holds no association. Every unit answered here carries the
 * name of its site: a single unit reads its site by primary key, which the persistence context
 * usually already holds because the caller checked the site's owner, and a list reads the names of
 * all its sites in one statement, never one per unit.</p>
 */
@Repository
public class EquipmentRepositoryImpl implements EquipmentRepository {

  private static final Sort LISTING_ORDER = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

  private final EquipmentPersistenceRepository equipmentPersistenceRepository;
  private final SitePersistenceRepository sitePersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final EntityManager entityManager;

  /**
   * Constructor
   *
   * @param equipmentPersistenceRepository the Spring Data repository
   * @param sitePersistenceRepository      the Spring Data repository of the sites, read for the name
   *                                       of the site a unit is installed at
   * @param eventPublisher                 the Spring event publisher used to dispatch domain events
   * @param entityManager                  the persistence context, flushed and cleared around the
   *                                       configuration version increment
   */
  public EquipmentRepositoryImpl(
      EquipmentPersistenceRepository equipmentPersistenceRepository,
      SitePersistenceRepository sitePersistenceRepository,
      ApplicationEventPublisher eventPublisher,
      EntityManager entityManager) {
    this.equipmentPersistenceRepository = equipmentPersistenceRepository;
    this.sitePersistenceRepository = sitePersistenceRepository;
    this.eventPublisher = eventPublisher;
    this.entityManager = entityManager;
  }

  // inherited javadoc
  @Override
  public Optional<Equipment> findById(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return equipmentPersistenceRepository.findById(equipmentId).map(this::toDomainWithSiteName);
  }

  // inherited javadoc
  @Override
  public List<Equipment> findBySiteId(Long siteId) {
    if (siteId == null) {
      return List.of();
    }
    return toDomainWithSiteNames(equipmentPersistenceRepository.findBySiteIdOrderByNameAsc(siteId));
  }

  /**
   * Returns one page of the units of an owner, with the count of every matching unit.
   *
   * <p>The rows and the count come from the same specification, so the total can never disagree
   * with the filters the rows were selected by. Spring Data applies the page in the database, which
   * is what keeps a paged listing from loading every matching row and discarding most of them in
   * memory, and it skips the count query whenever the page itself already tells the total.</p>
   *
   * <p>Rows are ordered by name and then by identifier, because a listing paginated without a
   * total order silently drops and repeats rows from one page to the next. A page whose first row
   * lies beyond what a JDBC offset can address is necessarily past the end, so it is answered
   * empty, with the real total, instead of failing.</p>
   *
   * @param ownerId       the owner identifier
   * @param siteId        restrict to a single site, may be {@code null}
   * @param status        restrict to a single status, may be {@code null}
   * @param equipmentType restrict to a single type, may be {@code null}
   * @param search        case-insensitive text matched against the name or the uid, may be
   *                      {@code null}
   * @param page          zero-based page index
   * @param size          maximum number of rows the page may hold
   * @return the rows of that page and the total count
   */
  @Override
  public EquipmentPage findPageByOwnerId(
      Long ownerId, Long siteId, StatusEquipment status, EquipmentType equipmentType, String search,
      int page, int size) {
    if (ownerId == null) {
      return new EquipmentPage(List.of(), page, size, 0L);
    }
    var specification = EquipmentPersistenceSpecifications.ownerListing(
        ownerId, siteId, status, equipmentType, search);
    if ((long) page * size > Integer.MAX_VALUE) {
      return new EquipmentPage(List.of(), page, size, equipmentPersistenceRepository.count(specification));
    }
    var slice = equipmentPersistenceRepository.findAll(specification, PageRequest.of(page, size, LISTING_ORDER));
    return new EquipmentPage(toDomainWithSiteNames(slice.getContent()), page, size, slice.getTotalElements());
  }

  /**
   * Returns every unit of an owner matching the filters, in the listing order.
   *
   * <p>Two statements whatever the number of units: one for the rows, one for the names of their
   * sites.</p>
   *
   * @param ownerId  the owner identifier
   * @param siteId   restrict to a single site, may be {@code null}
   * @param statuses restrict to these statuses; {@code null} or empty does not restrict
   * @return the matching units, by name and then by identifier
   */
  @Override
  public List<Equipment> findAllByOwnerId(Long ownerId, Long siteId, Set<StatusEquipment> statuses) {
    if (ownerId == null) {
      return List.of();
    }
    var specification = EquipmentPersistenceSpecifications.ownerDirectory(ownerId, siteId, statuses);
    return toDomainWithSiteNames(equipmentPersistenceRepository.findAll(specification, LISTING_ORDER));
  }

  // inherited javadoc
  @Override
  public boolean existsByUid(String uid) {
    return uid != null && !uid.isBlank() && equipmentPersistenceRepository.existsByEquipmentUid(uid.trim());
  }

  // inherited javadoc
  @Override
  public Optional<Equipment> findByUid(String uid) {
    if (uid == null || uid.isBlank()) {
      return Optional.empty();
    }
    return equipmentPersistenceRepository.findByEquipmentUid(uid.trim()).map(this::toDomainWithSiteName);
  }

  // inherited javadoc
  @Override
  public List<Equipment> findOnline() {
    return toDomainWithSiteNames(equipmentPersistenceRepository.findByOnlineTrueOrderByIdAsc());
  }

  /**
   * Persists a unit and dispatches whatever domain events the change raised.
   *
   * <p>The registration event is raised on the rehydrated copy rather than on the incoming
   * aggregate, because it carries the persistence identity and the incoming aggregate has none
   * yet; the events an update raised are read from the incoming aggregate instead, since they were
   * built from exactly what the caller asked for and rehydration normalizes values. Both sets are
   * therefore published, in that order, so a caller that registers a unit and immediately changes
   * its threshold emits the creation and the threshold change rather than only one of them.</p>
   *
   * <p>The site name the incoming aggregate was loaded with is carried over to the copy answered,
   * since saving a unit never moves it to another site; a unit that carries none, such as one just
   * built for registration, has it read.</p>
   *
   * @param equipment the unit to persist
   * @return the persisted unit
   */
  @Override
  public Equipment save(Equipment equipment) {
    var isNew = equipment.getEquipmentId() == null;
    var savedEntity = equipmentPersistenceRepository.save(
        EquipmentPersistenceAssembler.toPersistenceFromDomain(equipment));
    Equipment savedEquipment;
    if (equipment.getSiteName() == null) {
      savedEquipment = toDomainWithSiteName(savedEntity);
    } else {
      savedEquipment = EquipmentPersistenceAssembler.toDomainFromPersistence(savedEntity);
      savedEquipment.attachSiteName(equipment.getSiteName());
    }
    publishPendingEvents(equipment);
    if (isNew) {
      savedEquipment.onRegistered();
      publishPendingEvents(savedEquipment);
    }
    return savedEquipment;
  }

  /**
   * Increments the edge configuration version with a single Criteria API bulk statement, built by
   * {@link EquipmentPersistenceSpecifications#incrementConfigVersion}.
   *
   * <p>Pending writes are flushed first, so the statement runs after them, and the persistence
   * context is cleared afterward, so a unit read again in the same transaction carries the new
   * version instead of the copy cached before the statement.</p>
   *
   * @param equipmentId the unit identifier
   * @return the new version, or empty when the unit does not exist
   */
  @Override
  @Transactional
  public Optional<Integer> incrementConfigVersion(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    var increment = EquipmentPersistenceSpecifications.incrementConfigVersion(equipmentId, Instant.now());
    entityManager.flush();
    long updated;
    try {
      updated = equipmentPersistenceRepository.update(increment);
    } finally {
      entityManager.clear();
    }
    return updated == 0 ? Optional.empty() : findConfigVersion(equipmentId);
  }

  // inherited javadoc
  @Override
  public Optional<Integer> findConfigVersion(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return equipmentPersistenceRepository.findById(equipmentId).map(EquipmentPersistenceEntity::getConfigVersion);
  }

  /**
   * Rebuilds one unit and attaches the name of its site, read by primary key.
   *
   * @param entity the persisted row
   * @return the unit, carrying its site name when the site exists
   */
  private Equipment toDomainWithSiteName(EquipmentPersistenceEntity entity) {
    var equipment = EquipmentPersistenceAssembler.toDomainFromPersistence(entity);
    sitePersistenceRepository.findById(entity.getSiteId())
        .map(SitePersistenceEntity::getName)
        .ifPresent(equipment::attachSiteName);
    return equipment;
  }

  /**
   * Rebuilds a list of units and attaches the name of the site of each, reading the names of every
   * site involved in a single statement.
   *
   * @param entities the persisted rows, in the order to answer them
   * @return the units, in the same order
   */
  private List<Equipment> toDomainWithSiteNames(List<EquipmentPersistenceEntity> entities) {
    if (entities.isEmpty()) {
      return List.of();
    }
    var siteIds = entities.stream().map(EquipmentPersistenceEntity::getSiteId).collect(Collectors.toSet());
    var siteNames = sitePersistenceRepository.findAllById(siteIds).stream()
        .collect(Collectors.toMap(SitePersistenceEntity::getId, SitePersistenceEntity::getName));
    return entities.stream()
        .map(entity -> {
          var equipment = EquipmentPersistenceAssembler.toDomainFromPersistence(entity);
          equipment.attachSiteName(siteNames.get(entity.getSiteId()));
          return equipment;
        })
        .toList();
  }

  /**
   * Dispatches the events the aggregate registered, then clears them so a later save of the same
   * aggregate does not announce the same change twice.
   *
   * @param equipment the aggregate that was just persisted
   */
  private void publishPendingEvents(Equipment equipment) {
    equipment.domainEvents().forEach(eventPublisher::publishEvent);
    equipment.clearDomainEvents();
  }
}