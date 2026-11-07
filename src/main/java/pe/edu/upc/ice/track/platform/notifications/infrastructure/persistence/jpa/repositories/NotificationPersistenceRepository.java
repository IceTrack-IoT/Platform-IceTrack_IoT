package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;

/** Spring Data JPA repository operating on {@link NotificationPersistenceEntity}. */
public interface NotificationPersistenceRepository extends JpaRepository<NotificationPersistenceEntity, Long> {

  /**
   * Fetches one page of a recipient's notifications that were not dismissed.
   *
   * @param recipientUserId The recipient user Id
   * @param pageable        The page to read and its order
   * @return The page of notifications
   */
  Page<NotificationPersistenceEntity> findByRecipientUserIdAndDismissedAtIsNull(Long recipientUserId, Pageable pageable);

  /**
   * Fetches one page of a recipient's notifications neither read nor dismissed.
   *
   * @param recipientUserId The recipient user Id
   * @param pageable        The page to read and its order
   * @return The page of notifications
   */
  Page<NotificationPersistenceEntity> findByRecipientUserIdAndIsReadFalseAndDismissedAtIsNull(
      Long recipientUserId, Pageable pageable);

  /**
   * Counts a recipient's notifications neither read nor dismissed.
   *
   * @param recipientUserId The recipient user Id
   * @return The number of matching notifications
   */
  long countByRecipientUserIdAndIsReadFalseAndDismissedAtIsNull(Long recipientUserId);
}
