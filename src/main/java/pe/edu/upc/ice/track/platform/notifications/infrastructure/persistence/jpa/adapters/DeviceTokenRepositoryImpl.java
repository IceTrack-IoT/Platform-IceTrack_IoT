package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.DeviceTokenRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers.DeviceTokenPersistenceAssembler;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories.DeviceTokenPersistenceRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Adapter fulfilling {@link DeviceTokenRepository} on top of Spring Data JPA. */
@Repository
public class DeviceTokenRepositoryImpl implements DeviceTokenRepository {

  private final DeviceTokenPersistenceRepository persistenceRepository;

  public DeviceTokenRepositoryImpl(DeviceTokenPersistenceRepository persistenceRepository) {
    this.persistenceRepository = persistenceRepository;
  }

  @Override
  public DeviceToken save(DeviceToken deviceToken) {
    var saved = persistenceRepository.save(DeviceTokenPersistenceAssembler.toPersistenceEntityFromDomain(deviceToken));
    return DeviceTokenPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<DeviceToken> findByToken(String token) {
    return persistenceRepository.findByToken(token).map(DeviceTokenPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public List<DeviceToken> findByRecipientUserId(Long recipientUserId) {
    return persistenceRepository.findByRecipientUserId(recipientUserId).stream()
        .map(DeviceTokenPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  @Transactional
  public void deleteByTokenIn(Collection<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return;
    }
    persistenceRepository.deleteByTokenIn(tokens);
  }
}
