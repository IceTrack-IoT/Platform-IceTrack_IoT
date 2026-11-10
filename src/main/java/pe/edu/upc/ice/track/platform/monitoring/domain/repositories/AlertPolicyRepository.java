package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;

import java.util.Collection;
import java.util.List;
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

  /**
   *  Fetches the {@link AlertPolicy} of every unit, among the given ones, that has one of its own.
   * @param equipmentIds the identifiers of the units; empty matches nothing
   * @return  the policies bound to those units; a unit without a policy of its own contributes none
   */
  List<AlertPolicy> findByEquipmentIdIn(Collection<Long> equipmentIds);

  /** @return the platform-wide default policy, i.e. the one with a {@code null} equipmentId. */
  Optional<AlertPolicy> findDefaultPolicy();

  /**
   * Resolves the policy that applies to an equipment.
   *
   * @param equipmentId the identifier of the equipment
   * @return the equipment-specific policy, falling back to the platform-wide default; empty only
   *         when neither exists
   */
  default Optional<AlertPolicy> findApplicableTo(Long equipmentId) {
    return findByEquipmentId(equipmentId).or(this::findDefaultPolicy);
  }
}
