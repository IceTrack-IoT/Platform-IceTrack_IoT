package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPolicyPersistenceEntity;

import java.util.Optional;

/** Spring Data JPA repository operating on {@link AlertPolicyPersistenceEntity}. */
public interface AlertPolicyPersistenceRepository
    extends JpaRepository<AlertPolicyPersistenceEntity, Long> {

  /**
   *  Finds the alert policy associated with a given equipment.
   * @param equipmentId identifier of the equipment
   * @return  an {@link Optional} containing the alert policy if found, or empty if no policy is associated with the equipment
   */
  Optional<AlertPolicyPersistenceEntity> findByEquipmentId(Long equipmentId);

  /**
   *  Finds the default alert policy that is not associated with any specific equipment.
   * @return  an {@link Optional} containing the default alert policy if found, or empty if no default policy exists
   */
  Optional<AlertPolicyPersistenceEntity> findByEquipmentIdIsNull();
}
