package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.TechnicianCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers.TechnicianProfilePersistenceAssembler;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.TechnicianProfilePersistenceRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the technician profile repository port with Spring Data JPA.
 *
 * <p>Also acts as the event-publishing boundary: after a brand-new {@link TechnicianProfile} is
 * persisted (and its identity is therefore available), a {@link TechnicianCreatedEvent} is
 * dispatched via Spring's {@link ApplicationEventPublisher}.</p>
 */
@Repository
public class TechnicianProfileRepositoryImpl implements TechnicianProfileRepository {

  private final TechnicianProfilePersistenceRepository technicianProfilePersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TechnicianProfileRepositoryImpl(
      TechnicianProfilePersistenceRepository technicianProfilePersistenceRepository,
      ApplicationEventPublisher eventPublisher) {
    this.technicianProfilePersistenceRepository = technicianProfilePersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Optional<TechnicianProfile> findById(Long userProfileId) {
    if (userProfileId == null) return Optional.empty();
    return technicianProfilePersistenceRepository.findById(userProfileId)
        .map(TechnicianProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<TechnicianProfile> findByUserId(UserId userId) {
    if (userId == null || userId.userId() == null) return Optional.empty();
    return technicianProfilePersistenceRepository.findByUserId(userId.userId())
        .map(TechnicianProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public List<TechnicianProfile> findAll() {
    return technicianProfilePersistenceRepository.findAll().stream()
        .map(TechnicianProfilePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  @Override
  public TechnicianProfile save(TechnicianProfile technician) {
    boolean isNew = technician.getUserProfileId() == null;
    var savedEntity = technicianProfilePersistenceRepository.save(
        TechnicianProfilePersistenceAssembler.toPersistenceFromDomain(technician));
    var savedTechnician = TechnicianProfilePersistenceAssembler.toDomainFromPersistence(savedEntity);
    if (isNew) {
      savedTechnician.onCreated();
      savedTechnician.domainEvents().forEach(eventPublisher::publishEvent);
      savedTechnician.clearDomainEvents();
    }
    return savedTechnician;
  }
}
