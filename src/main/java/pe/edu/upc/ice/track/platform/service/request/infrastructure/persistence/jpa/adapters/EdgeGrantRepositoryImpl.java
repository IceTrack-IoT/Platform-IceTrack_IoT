package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.EdgeGrantRepository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.assemblers.EdgeGrantPersistenceAssembler;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories.EdgeGrantPersistenceRepository;

import java.time.Instant;
import java.util.List;

/**
 * Repository adapter that bridges the {@link EdgeGrantRepository} port with Spring Data JPA.
 *
 * <p>The domain events a grant registered are published right after its row is written, so a grant
 * change announced to the rest of the platform is always one that reached the database.</p>
 */
@Repository
public class EdgeGrantRepositoryImpl implements EdgeGrantRepository {

  private final EdgeGrantPersistenceRepository persistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public EdgeGrantRepositoryImpl(
      EdgeGrantPersistenceRepository persistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.persistenceRepository = persistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public EdgeGrant save(EdgeGrant grant) {
    var saved = persistenceRepository.save(EdgeGrantPersistenceAssembler.toPersistenceFromDomain(grant));
    grant.domainEvents().forEach(eventPublisher::publishEvent);
    grant.clearDomainEvents();
    return EdgeGrantPersistenceAssembler.toDomainFromPersistence(saved);
  }

  @Override
  public List<EdgeGrant> findUnrevokedByServiceRequestId(Long serviceRequestId) {
    if (serviceRequestId == null) {
      return List.of();
    }
    return persistenceRepository.findByServiceRequestIdAndRevokedAtIsNull(serviceRequestId).stream()
        .map(EdgeGrantPersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  @Override
  public List<EdgeGrant> findActiveByEquipmentId(Long equipmentId, Instant at) {
    if (equipmentId == null || at == null) {
      return List.of();
    }
    return persistenceRepository.findByEquipmentIdAndRevokedAtIsNullAndExpiresAtAfterOrderByIdAsc(equipmentId, at)
        .stream()
        .map(EdgeGrantPersistenceAssembler::toDomainFromPersistence)
        .toList();
  }
}
