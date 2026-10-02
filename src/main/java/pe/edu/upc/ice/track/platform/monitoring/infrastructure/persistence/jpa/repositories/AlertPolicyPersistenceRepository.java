package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPolicyPersistenceEntity;

import java.util.Optional;

/** Spring Data JPA repository operating on {@link AlertPolicyPersistenceEntity}. */
public interface AlertPolicyPersistenceRepository
    extends JpaRepository<AlertPolicyPersistenceEntity, Long> {

  Optional<AlertPolicyPersistenceEntity> findByEquipmentId(Long equipmentId);

  Optional<AlertPolicyPersistenceEntity> findByEquipmentIdIsNull();
}
