package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.SitePersistenceEntity;

import java.util.List;

/**
 * Spring Data repository for {@link SitePersistenceEntity}.
 *
 * <p>Owner-scoped listings resolve through the {@code idx_sites_owner_profiles_id} index declared
 * on the entity.</p>
 */
@Repository
public interface SitePersistenceRepository extends JpaRepository<SitePersistenceEntity, Long> {

  /**
   * Finds every site belonging to an owner.
   *
   * @param ownerProfilesId the owner's profile identifier
   * @return the rows of that owner, empty when it has none
   */
  List<SitePersistenceEntity> findByOwnerProfilesIdOrderByNameAsc(Long ownerProfilesId);

  /**
   * Finds every site in a given geocoding status.
   *
   * @param geocodingStatus the status to match
   * @return the matching rows, empty when there are none
   */
  List<SitePersistenceEntity> findByGeocodingStatus(GeocodingStatus geocodingStatus);
}
