package pe.edu.upc.ice.track.platform.assets.domain.repositories;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

import java.util.List;
import java.util.Optional;

/**
 * Equipment repository port.
 *
 * <p>Implemented by {@code EquipmentRepositoryImpl} in the infrastructure layer, which is also the
 * place where the {@code EquipmentRegisteredEvent} of a brand new unit is published.</p>
 *
 * <p>Owner-scoped lookups join {@code equipments} with {@code sites}: a unit has no owner column
 * of its own, it belongs to whoever owns the site it is installed at. Joining here, once, keeps
 * that rule in a single place instead of spreading it across the query services.</p>
 *
 * <p>Pagination is expressed as an {@code offset}/{@code limit} pair plus a separate count rather
 * than through a framework page type, so that this port stays free of any framework type.</p>
 */
public interface EquipmentRepository {

  /**
   * Find a unit by its identifier, whatever its owner.
   *
   * @param equipmentId the unit identifier
   * @return the unit, or empty when no unit carries that identifier
   */
  Optional<Equipment> findById(Long equipmentId);

  /**
   * Find every unit installed at a site.
   *
   * @param siteId the site identifier
   * @return the units of that site, empty when it has none
   */
  List<Equipment> findBySiteId(Long siteId);

  /**
   * Find one page of the units of an owner, optionally narrowed by site, status and type.
   *
   * <p>A {@code null} filter does not restrict that dimension. This is the query behind the
   * dashboard listing and the US-11 filters, so it is served by the indexes on {@code site_id},
   * on {@code (status, equipment_type)} and by the join on the owner's
   * {@code owner_profiles_id}.</p>
   *
   * @param ownerId       the owner identifier; required
   * @param siteId        restrict to a single site, may be {@code null}
   * @param status        restrict to a single status, may be {@code null}
   * @param equipmentType restrict to a single type, may be {@code null}
   * @param offset        zero-based index of the first row to return; not negative
   * @param limit         maximum number of rows to return; strictly positive
   * @return the matching units of that page
   */
  List<Equipment> findByOwnerId(
      Long ownerId, Long siteId, StatusEquipment status, EquipmentType equipmentType, int offset, int limit);

  /**
   * Count the units of an owner matching the very same filters, to size the pages of a listing.
   *
   * @param ownerId       the owner identifier; required
   * @param siteId        restrict to a single site, may be {@code null}
   * @param status        restrict to a single status, may be {@code null}
   * @param equipmentType restrict to a single type, may be {@code null}
   * @return the number of matching units, ignoring paging
   */
  long countByOwnerId(Long ownerId, Long siteId, StatusEquipment status, EquipmentType equipmentType);

  /**
   * Check whether a unit's own identifier is already taken.
   *
   * <p>Backs the uniqueness rule on {@code equipment_uid}: the check runs before the insert so
   * the caller gets a conflict rather than a constraint violation, while the unique constraint on
   * the column remains the real guarantee under concurrency.</p>
   *
   * @param uid the unit's own identifier
   * @return {@code true} when some unit already uses it
   */
  boolean existsByUid(String uid);

  /**
   * Find a unit by the uid printed on the device.
   *
   * <p>Exists for Device Management, which learns about a unit from the board itself when it
   * reports the uid it was flashed with, and has no identifier of its own to search by.</p>
   *
   * @param uid the unit's own identifier, unique across the platform
   * @return the unit, or empty when no unit carries that uid
   */
  Optional<Equipment> findByUid(String uid);

  /**
   * Persist a unit, inserting it when it is new and updating it otherwise.
   *
   * @param equipment the unit to persist
   * @return the persisted unit, carrying its identity and having published its registration event
   *         when it was new
   */
  Equipment save(Equipment equipment);
}