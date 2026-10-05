package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;

import java.util.Optional;

/**
 * Domain repository port for {@link AlertPolicy}.
 *
 * <p>Implemented by {@code AlertPolicyRepositoryImpl} in the infrastructure layer.</p>
 */
public interface AlertPolicyRepository {

  /**
   *  Persists the given {@link AlertPolicy} in the repository.
   * @param policy  the policy to persist
   * @return  the persisted policy, with any generated identifiers populated
   */
  AlertPolicy save(AlertPolicy policy);

  /**
   *  Fetches the {@link AlertPolicy} associated with the given equipment identifier.
   * @param equipmentId the identifier of the equipment
   * @return  an {@link Optional} containing the policy if found, or empty if no policy is associated with the equipment
   */
  Optional<AlertPolicy> findByEquipmentId(Long equipmentId);

  /** @return the platform-wide default policy, i.e. the one with a {@code null} equipmentId. */
  Optional<AlertPolicy> findDefaultPolicy();
}
