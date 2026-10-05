package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link EquipmentPersistenceEntity}.
 *
 * <p>A unit carries no owner column of its own - it belongs to whoever owns its site - so every
 * owner-scoped lookup has to go through {@code sites}. The queries below express that as a subquery
 * on the site's identifier, which is both portable across Hibernate versions and index-friendly:
 * {@code assets.sites} is probed by its primary key and the equipment table is then filtered on
 * {@code site_id}, rather than being driven by a cross join whose plan depends on the optimiser.</p>
 */
@Repository
public interface EquipmentPersistenceRepository extends JpaRepository<EquipmentPersistenceEntity, Long> {

  /**
   * Finds the units installed at a site, ordered by name so a listing is stable.
   *
   * @param siteId the site identifier
   * @return the rows of that site, empty when it has none
   */
  List<EquipmentPersistenceEntity> findBySiteIdOrderByNameAsc(Long siteId);

  /**
   * Finds a unit by the uid printed on the device.
   *
   * @param equipmentUid the unit's own identifier
   * @return the row, or empty when no unit carries that uid
   */
  Optional<EquipmentPersistenceEntity> findByEquipmentUid(String equipmentUid);

  /**
   * Tells whether a uid is already taken.
   *
   * @param equipmentUid the unit's own identifier
   * @return {@code true} when some unit already uses it
   */
  boolean existsByEquipmentUid(String equipmentUid);

  /**
   * Returns one page of the units of an owner, narrowed by the optional filters.
   *
   * <p>Each filter is written as {@code (:p IS NULL OR column = :p)} so that omitting one does
   * not change the shape of the statement: the database still plans a single query and picks the
   * matching index, instead of the application assembling a different statement per combination
   * of filters.</p>
   *
   * <p>Ordered by name and then by identifier, because a listing paginated without a total order
   * silently drops and repeats rows from one page to the next.</p>
   *
   * @param ownerProfilesId the owner's profile identifier
   * @param siteId          restrict to a single site, or {@code null}
   * @param status          restrict to a single status, or {@code null}
   * @param equipmentType   restrict to a single type, or {@code null}
   * @param pageable        the requested slice
   * @return the rows of that page
   */
  @Query("""
      SELECT e FROM EquipmentPersistenceEntity e
      WHERE e.siteId IN (SELECT s.id FROM SitePersistenceEntity s WHERE s.ownerProfilesId = :ownerProfilesId)
        AND (:siteId IS NULL OR e.siteId = :siteId)
        AND (:status IS NULL OR e.status = :status)
        AND (:equipmentType IS NULL OR e.equipmentType = :equipmentType)
      ORDER BY e.name ASC, e.id ASC
      """)
  List<EquipmentPersistenceEntity> findPageByOwner(
      @Param("ownerProfilesId") Long ownerProfilesId,
      @Param("siteId") Long siteId,
      @Param("status") StatusEquipment status,
      @Param("equipmentType") EquipmentType equipmentType,
      Pageable pageable);

  /**
   * Counts the units of an owner matching the very same filters, to size the pages of a listing.
   *
   * @param ownerProfilesId the owner's profile identifier
   * @param siteId          restrict to a single site, or {@code null}
   * @param status          restrict to a single status, or {@code null}
   * @param equipmentType   restrict to a single type, or {@code null}
   * @return the number of matching rows, ignoring paging
   */
  @Query("""
      SELECT COUNT(e) FROM EquipmentPersistenceEntity e
      WHERE e.siteId IN (SELECT s.id FROM SitePersistenceEntity s WHERE s.ownerProfilesId = :ownerProfilesId)
        AND (:siteId IS NULL OR e.siteId = :siteId)
        AND (:status IS NULL OR e.status = :status)
        AND (:equipmentType IS NULL OR e.equipmentType = :equipmentType)
      """)
  long countByOwner(
      @Param("ownerProfilesId") Long ownerProfilesId,
      @Param("siteId") Long siteId,
      @Param("status") StatusEquipment status,
      @Param("equipmentType") EquipmentType equipmentType);
}