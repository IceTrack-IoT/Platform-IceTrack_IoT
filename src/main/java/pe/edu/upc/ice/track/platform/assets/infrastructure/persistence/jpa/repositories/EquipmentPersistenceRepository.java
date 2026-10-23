package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link EquipmentPersistenceEntity}.
 *
 * <p>A unit carries no owner column of its own - it belongs to whoever owns its site - so every
 * owner-scoped lookup has to go through {@code sites}. The owner listing is served through
 * {@link JpaSpecificationExecutor} with the specification built by
 * {@code EquipmentPersistenceSpecifications}, which expresses that rule as a subquery on the site's
 * identifier: portable across Hibernate versions and index-friendly, since {@code sites} is probed
 * through its owner index and the equipment table is then filtered on {@code site_id}, rather than
 * being driven by a cross join whose plan depends on the optimizer.</p>
 */
@Repository
public interface EquipmentPersistenceRepository
    extends JpaRepository<EquipmentPersistenceEntity, Long>, JpaSpecificationExecutor<EquipmentPersistenceEntity> {

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
}
