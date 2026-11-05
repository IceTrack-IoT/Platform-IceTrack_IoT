package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.DeviceTokenPersistenceEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Spring Data JPA repository operating on {@link DeviceTokenPersistenceEntity}. */
public interface DeviceTokenPersistenceRepository extends JpaRepository<DeviceTokenPersistenceEntity, Long> {

  Optional<DeviceTokenPersistenceEntity> findByToken(String token);

  List<DeviceTokenPersistenceEntity> findByRecipientUserId(Long recipientUserId);

  /**
   * Deletes the rows carrying any of the given tokens.
   *
   * <p>A derived delete: Spring Data loads the matching rows and removes each one, so it must run
   * inside a transaction, which {@code DeviceTokenRepositoryImpl#deleteByTokenIn} opens.</p>
   *
   * @param tokens the push tokens to delete
   */
  void deleteByTokenIn(Collection<String> tokens);
}
