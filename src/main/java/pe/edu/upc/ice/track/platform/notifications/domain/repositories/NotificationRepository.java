package pe.edu.upc.ice.track.platform.notifications.domain.repositories;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository port for {@link Notification}.
 *
 * <p>Implemented by {@code NotificationRepositoryImpl} in the infrastructure layer, which
 * delegates to the Spring Data JPA repository and the persistence assembler.</p>
 */
public interface NotificationRepository {

  /**
   *  Persists a notification in the database.
   * @param notification The notification Id
   * @return  The persisted notification with its generated identifier.
   */
  Notification save(Notification notification);

  /**
   *  Fetches a notification by its identifier.
   * @param notificationId  The notification Id
   * @return  An {@code Optional} containing the notification if found, or empty if not found.
   */
  Optional<Notification> findById(Long notificationId);

  /**
   *  Fetches all notifications for a given recipient user.
   * @param recipientUserId The recipient user Id
   * @return  A list of notifications for the given recipient user.
   */
  List<Notification> findByRecipientUserId(Long recipientUserId);

  /**
   *  Fetches all unread notifications for a given recipient user.
   * @param recipientUserId The recipient user Id
   * @return  A list of unread notifications for the given recipient user.
   */
  List<Notification> findByRecipientUserIdAndIsReadFalse(Long recipientUserId);
}
