package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.ProfileCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers.ProfilePersistenceAssembler;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.ProfilePersistenceRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the domain profile repository port with Spring Data JPA.
 *
 * <p>Also acts as the event-publishing boundary: after a brand-new {@link Profile} is
 * persisted (and the JPA-assigned id is therefore available), a {@link ProfileCreatedEvent}
 * is dispatched via Spring's {@link ApplicationEventPublisher}.</p>
 */
@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

  private final ProfilePersistenceRepository profilePersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ProfileRepositoryImpl(
      ProfilePersistenceRepository profilePersistenceRepository,
      ApplicationEventPublisher eventPublisher) {
    this.profilePersistenceRepository = profilePersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Optional<Profile> findById(Long id) {
    return profilePersistenceRepository.findById(id).map(ProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<Profile> findByEmailAddress(EmailAddress emailAddress) {
    if (emailAddress == null) return Optional.empty();
    return profilePersistenceRepository.findByEmailAddress(emailAddress).map(ProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<Profile> findByUserId(UserId userId) {
    if (userId == null || userId.userId() == null) return Optional.empty();
    return profilePersistenceRepository.findByUserId(userId.userId()).map(ProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public List<Profile> findAll() {
    return profilePersistenceRepository.findAll().stream().map(ProfilePersistenceAssembler::toDomainFromPersistence).toList();
  }

  @Override
  public Profile save(Profile profile) {
    boolean isNew = profile.getId() == null;
    var savedEntity = profilePersistenceRepository.save(ProfilePersistenceAssembler.toPersistenceFromDomain(profile));
    var savedProfile = ProfilePersistenceAssembler.toDomainFromPersistence(savedEntity);
    if (isNew) {
      savedProfile.onCreated();
      savedProfile.domainEvents().forEach(eventPublisher::publishEvent);
      savedProfile.clearDomainEvents();
    }
    return savedProfile;
  }

  @Override
  public boolean existsByEmailAddress(EmailAddress emailAddress) {
    return emailAddress != null && profilePersistenceRepository.countByEmailAddress(emailAddress) > 0;
  }

  @Override
  public boolean existsByUserId(UserId userId) {
    return userId != null && userId.userId() != null && profilePersistenceRepository.existsByUserId(userId.userId());
  }
}
