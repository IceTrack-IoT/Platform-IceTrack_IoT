package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

import java.util.List;
import java.util.Optional;

/** Spring Data JPA repository operating on {@link AlertPersistenceEntity}. */
public interface AlertPersistenceRepository extends JpaRepository<AlertPersistenceEntity, Long> {

  List<AlertPersistenceEntity> findByEquipmentIdAndStatusIn(Long equipmentId, List<String> statuses);

  Optional<AlertPersistenceEntity> findFirstByEquipmentIdAndTypeAndStatusIn(
      Long equipmentId, String type, List<String> openStatuses);
}
