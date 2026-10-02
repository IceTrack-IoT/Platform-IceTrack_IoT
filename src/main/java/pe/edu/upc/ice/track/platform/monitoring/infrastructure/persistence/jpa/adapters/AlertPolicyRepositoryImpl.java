package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.AlertPolicyPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.AlertPolicyPersistenceRepository;

import java.util.Optional;

/** Adapter fulfilling {@link AlertPolicyRepository} on top of Spring Data JPA. */
@Repository
public class AlertPolicyRepositoryImpl implements AlertPolicyRepository {

  private final AlertPolicyPersistenceRepository persistenceRepository;

  public AlertPolicyRepositoryImpl(AlertPolicyPersistenceRepository persistenceRepository) {
    this.persistenceRepository = persistenceRepository;
  }

  @Override
  public AlertPolicy save(AlertPolicy policy) {
    var entity = AlertPolicyPersistenceAssembler.toPersistenceEntityFromDomain(policy);
    var saved = persistenceRepository.save(entity);
    return AlertPolicyPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<AlertPolicy> findByEquipmentId(Long equipmentId) {
    return persistenceRepository.findByEquipmentId(equipmentId)
        .map(AlertPolicyPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public Optional<AlertPolicy> findDefaultPolicy() {
    return persistenceRepository.findByEquipmentIdIsNull()
        .map(AlertPolicyPersistenceAssembler::toDomainFromPersistenceEntity);
  }
}
