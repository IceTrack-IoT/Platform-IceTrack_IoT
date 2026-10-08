package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;

import java.util.List;

/** Spring Data JPA repository operating on {@link NotificationPersistenceEntity}. */
public interface NotificationPersistenceRepository extends JpaRepository<NotificationPersistenceEntity, Long> {

  /**
   *  Fetches all notifications for a given recipient user.
   * @param recipientUserId The recipient user Id
   * @return  A list of notifications for the given recipient user.
   */
  List<NotificationPersistenceEntity> findByRecipientUserId(Long recipientUserId);

  /**
   *  Fetches all unread notifications for a given recipient user.
   * @param recipientUserId The recipient user Id
   * @return  A list of unread notifications for the given recipient user.
   */
  List<NotificationPersistenceEntity> findByRecipientUserIdAndIsReadFalse(Long recipientUserId);
}
