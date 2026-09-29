package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;

import java.util.Optional;

/**
 * Domain repository port for {@link AlertPolicy}.
 *
 * <p>Implemented by {@code AlertPolicyRepositoryImpl} in the infrastructure layer.</p>
 */
public interface AlertPolicyRepository {

  AlertPolicy save(AlertPolicy policy);

  Optional<AlertPolicy> findByEquipmentId(Long equipmentId);

  /** @return the platform-wide default policy, i.e. the one with a {@code null} equipmentId. */
  Optional<AlertPolicy> findDefaultPolicy();
}
