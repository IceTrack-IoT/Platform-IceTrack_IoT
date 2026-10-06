package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers.ProfilePersistenceAssembler;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.ProfilePersistenceRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Repository adapter that bridges the role-agnostic profile repository port with Spring Data JPA.
 */
@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

  private final ProfilePersistenceRepository profilePersistenceRepository;

  public ProfileRepositoryImpl(ProfilePersistenceRepository profilePersistenceRepository) {
    this.profilePersistenceRepository = profilePersistenceRepository;
  }

  @Override
  public Optional<Profile> findByUserId(UserId userId) {
    if (userId == null || userId.userId() == null) return Optional.empty();
    return profilePersistenceRepository.findByUserId(userId.userId())
        .map(ProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public boolean existsByUserId(UserId userId) {
    return userId != null && userId.userId() != null && profilePersistenceRepository.existsByUserId(userId.userId());
  }

  @Override
  public Optional<Profile> findByEmailAddress(EmailAddress emailAddress) {
    if (emailAddress == null) return Optional.empty();
    return profilePersistenceRepository.findByEmailAddress(emailAddress)
        .map(ProfilePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public boolean existsByEmailAddress(EmailAddress emailAddress) {
    return emailAddress != null && profilePersistenceRepository.countByEmailAddress(emailAddress) > 0;
  }
}
