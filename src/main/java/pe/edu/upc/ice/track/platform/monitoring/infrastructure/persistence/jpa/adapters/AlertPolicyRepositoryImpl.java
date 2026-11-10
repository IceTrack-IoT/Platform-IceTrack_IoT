package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers.AlertPolicyPersistenceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.repositories.AlertPolicyPersistenceRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adapter fulfilling {@link AlertPolicyRepository} on top of Spring Data JPA.
 *
 * <p>Dispatches the domain events a policy registered right after its row is written, so a change
 * announced to the rest of the platform is always one that reached the database.</p>
 */
@Repository
public class AlertPolicyRepositoryImpl implements AlertPolicyRepository {

  private final AlertPolicyPersistenceRepository persistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public AlertPolicyRepositoryImpl(
      AlertPolicyPersistenceRepository persistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.persistenceRepository = persistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public AlertPolicy save(AlertPolicy policy) {
    var entity = AlertPolicyPersistenceAssembler.toPersistenceEntityFromDomain(policy);
    var saved = persistenceRepository.save(entity);
    policy.domainEvents().forEach(eventPublisher::publishEvent);
    policy.clearDomainEvents();
    return AlertPolicyPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<AlertPolicy> findByEquipmentId(Long equipmentId) {
    return persistenceRepository.findByEquipmentId(equipmentId)
        .map(AlertPolicyPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public List<AlertPolicy> findByEquipmentIdIn(Collection<Long> equipmentIds) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return List.of();
    }
    return persistenceRepository.findByEquipmentIdIn(equipmentIds).stream()
        .map(AlertPolicyPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  public Optional<AlertPolicy> findDefaultPolicy() {
    return persistenceRepository.findByEquipmentIdIsNull()
        .map(AlertPolicyPersistenceAssembler::toDomainFromPersistenceEntity);
  }
}
