package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers.EquipmentPersistenceAssembler;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories.EquipmentPersistenceRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.specifications.EquipmentPersistenceSpecifications;

import java.util.List;
import java.util.Optional;

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
 */
@Repository
public class EquipmentRepositoryImpl implements EquipmentRepository {

  private static final Sort LISTING_ORDER = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

  private final EquipmentPersistenceRepository equipmentPersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor
   *
   * @param equipmentPersistenceRepository the Spring Data repository
   * @param eventPublisher                 the Spring event publisher used to dispatch domain events
   */
  public EquipmentRepositoryImpl(
      EquipmentPersistenceRepository equipmentPersistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.equipmentPersistenceRepository = equipmentPersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  // inherited javadoc
  @Override
  public Optional<Equipment> findById(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return equipmentPersistenceRepository.findById(equipmentId)
        .map(EquipmentPersistenceAssembler::toDomainFromPersistence);
  }

  // inherited javadoc
  @Override
  public List<Equipment> findBySiteId(Long siteId) {
    if (siteId == null) {
      return List.of();
    }
    return equipmentPersistenceRepository.findBySiteIdOrderByNameAsc(siteId).stream()
        .map(EquipmentPersistenceAssembler::toDomainFromPersistence)
        .toList();
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
    var content = slice.getContent().stream()
        .map(EquipmentPersistenceAssembler::toDomainFromPersistence)
        .toList();
    return new EquipmentPage(content, page, size, slice.getTotalElements());
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
    return equipmentPersistenceRepository.findByEquipmentUid(uid.trim())
        .map(EquipmentPersistenceAssembler::toDomainFromPersistence);
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
   * @param equipment the unit to persist
   * @return the persisted unit
   */
  @Override
  public Equipment save(Equipment equipment) {
    var isNew = equipment.getEquipmentId() == null;
    var savedEntity = equipmentPersistenceRepository.save(
        EquipmentPersistenceAssembler.toPersistenceFromDomain(equipment));
    var savedEquipment = EquipmentPersistenceAssembler.toDomainFromPersistence(savedEntity);
    publishPendingEvents(equipment);
    if (isNew) {
      savedEquipment.onRegistered();
      publishPendingEvents(savedEquipment);
    }
    return savedEquipment;
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