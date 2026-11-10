package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;

/**
 * Spring Data JPA repository operating on {@link NotificationPersistenceEntity}.
 *
 * <p>The inbox page and the bulk read transitions go through {@link JpaSpecificationExecutor} with
 * the specifications built by {@code NotificationPersistenceSpecifications}.
 * {@code NotificationRepositoryImpl} flushes pending writes before each bulk update and clears the
 * persistence context afterward, so no stale managed instance survives it.</p>
 */
public interface NotificationPersistenceRepository
    extends JpaRepository<NotificationPersistenceEntity, Long>,
    JpaSpecificationExecutor<NotificationPersistenceEntity> {

  /**
   * Counts a recipient's notifications neither read nor dismissed.
   *
   * @param recipientUserId The recipient user Id
   * @return The number of matching notifications
   */
  long countByRecipientUserIdAndIsReadFalseAndDismissedAtIsNull(Long recipientUserId);
}
