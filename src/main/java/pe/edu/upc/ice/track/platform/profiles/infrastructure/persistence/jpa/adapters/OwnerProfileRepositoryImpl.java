package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.OwnerCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.OwnerProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers.OwnerProfilePersistenceAssembler;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.OwnerProfilePersistenceRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the owner profile repository port with Spring Data JPA.
 *
 * <p>Also acts as the event-publishing boundary: after a brand-new {@link OwnerProfile} is
 * persisted (and its identity is therefore available), an {@link OwnerCreatedEvent} is dispatched
 * via Spring's {@link ApplicationEventPublisher}.</p>
 */
@Repository
public class OwnerProfileRepositoryImpl implements OwnerProfileRepository {

  private final OwnerProfilePersistenceRepository ownerProfilePersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public OwnerProfileRepositoryImpl(
      OwnerProfilePersistenceRepository ownerProfilePersistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.ownerProfilePersistenceRepository = ownerProfilePersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Optional<OwnerProfile> findById(Long userProfileId) {
    if (userProfileId == null) return Optional.empty();
    return ownerProfilePersistenceRepository.findById(userProfileId)
        .map(OwnerProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<OwnerProfile> findByUserId(UserId userId) {
    if (userId == null || userId.userId() == null) return Optional.empty();
    return ownerProfilePersistenceRepository.findByUserId(userId.userId())
        .map(OwnerProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public List<OwnerProfile> findAll() {
    return ownerProfilePersistenceRepository.findAll().stream()
        .map(OwnerProfilePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  @Override
  public OwnerProfile save(OwnerProfile owner) {
    boolean isNew = owner.getUserProfileId() == null;
    var savedEntity = ownerProfilePersistenceRepository.save(OwnerProfilePersistenceAssembler.toPersistenceFromDomain(owner));
    var savedOwner = OwnerProfilePersistenceAssembler.toDomainFromPersistence(savedEntity);
    if (isNew) {
      savedOwner.onCreated();
      savedOwner.domainEvents().forEach(eventPublisher::publishEvent);
      savedOwner.clearDomainEvents();
    }
    return savedOwner;
  }
}
