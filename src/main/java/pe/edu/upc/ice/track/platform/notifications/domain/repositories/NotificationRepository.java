package pe.edu.upc.ice.track.platform.notifications.domain.repositories;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;

import java.util.Optional;

/**
 * Domain repository port for {@link Notification}.
 *
 * <p>Implemented by {@code NotificationRepositoryImpl} in the infrastructure layer, which
 * delegates to the Spring Data JPA repository and the persistence assembler.</p>
 *
 * <p>"Active" means not dismissed: a dismissed notification is hidden from the inbox but kept.</p>
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
   * Fetches one page of a recipient's active notifications, most recent first.
   *
   * @param recipientUserId the recipient user Id
   * @param unreadOnly      {@code true} to keep only notifications not yet read
   * @param page            zero-based page index
   * @param size            maximum number of notifications on the page
   * @return the notifications of that page and the total count
   */
  NotificationPage findActivePageByRecipientUserId(Long recipientUserId, boolean unreadOnly, int page, int size);

  /**
   * Counts a recipient's active notifications not yet read.
   *
   * @param recipientUserId the recipient user Id
   * @return the number of notifications neither read nor dismissed
   */
  long countActiveUnreadByRecipientUserId(Long recipientUserId);
}
