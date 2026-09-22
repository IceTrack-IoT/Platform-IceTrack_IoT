package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers.UserPersistenceAssembler;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the IAM user domain repository port with Spring Data JPA.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

  private final UserPersistenceRepository userPersistenceRepository;

  public UserRepositoryImpl(UserPersistenceRepository userPersistenceRepository) {
    this.userPersistenceRepository = userPersistenceRepository;
  }

  @Override
  public Optional<User> findById(Long id) {
    return userPersistenceRepository.findById(id).map(UserPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return userPersistenceRepository.findByUsername(username).map(UserPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    if (email == null || email.isBlank()) return Optional.empty();
    return userPersistenceRepository.findByEmail(email).map(UserPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<User> findByProviderAndExternalId(AuthProvider provider, String externalId) {
    if (provider == null || externalId == null || externalId.isBlank()) return Optional.empty();
    return userPersistenceRepository.findByProviderAndExternalId(provider, externalId)
        .map(UserPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public List<User> findAll() {
    return userPersistenceRepository.findAll().stream().map(UserPersistenceAssembler::toDomainFromPersistence).toList();
  }

  @Override
  public User save(User user) {
    var saved = userPersistenceRepository.save(UserPersistenceAssembler.toPersistenceFromDomain(user));
    return UserPersistenceAssembler.toDomainFromPersistence(saved);
  }

  @Override
  public boolean existsByUsername(String username) {
    return userPersistenceRepository.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return email != null && !email.isBlank() && userPersistenceRepository.existsByEmail(email);
  }
}
