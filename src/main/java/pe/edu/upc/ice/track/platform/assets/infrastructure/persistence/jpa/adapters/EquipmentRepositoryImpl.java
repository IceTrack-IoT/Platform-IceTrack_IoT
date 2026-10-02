package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers.EquipmentPersistenceAssembler;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories.EquipmentPersistenceRepository;

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
   * Returns one slice of the units of an owner.
   *
   * <p>The offset and the limit handed in by the port are turned back into a page index so that
   * Spring Data can apply them in the database, which is what keeps a paged listing from loading
   * every matching row and discarding most of them in memory.</p>
   *
   * @param ownerId       the owner identifier
   * @param siteId        restrict to a single site, may be {@code null}
   * @param status        restrict to a single status, may be {@code null}
   * @param equipmentType restrict to a single type, may be {@code null}
   * @param offset        zero-based index of the first row to return
   * @param limit         maximum number of rows to return
   * @return the rows of that slice
   */
  @Override
  public List<Equipment> findByOwnerId(
      Long ownerId, Long siteId, StatusEquipment status, EquipmentType equipmentType, int offset, int limit) {
    if (ownerId == null || offset < 0 || limit <= 0) {
      return List.of();
    }
    var page = PageRequest.of(offset / limit, limit);
    return equipmentPersistenceRepository
        .findPageByOwner(ownerId, siteId, status, equipmentType, page)
        .stream()
        .map(EquipmentPersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  // inherited javadoc
  @Override
  public long countByOwnerId(Long ownerId, Long siteId, StatusEquipment status, EquipmentType equipmentType) {
    if (ownerId == null) {
      return 0L;
    }
    return equipmentPersistenceRepository.countByOwner(ownerId, siteId, status, equipmentType);
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
   * built from exactly what the caller asked for and rehydration normalises values. Both sets are
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