package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.DeviceTokenPersistenceEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Spring Data JPA repository operating on {@link DeviceTokenPersistenceEntity}. */
public interface DeviceTokenPersistenceRepository extends JpaRepository<DeviceTokenPersistenceEntity, Long> {

  Optional<DeviceTokenPersistenceEntity> findByToken(String token);

  List<DeviceTokenPersistenceEntity> findByRecipientUserId(Long recipientUserId);

  @Modifying
  @Query("delete from DeviceTokenPersistenceEntity d where d.token in :tokens")
  void deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
