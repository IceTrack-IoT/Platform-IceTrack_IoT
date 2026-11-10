package pe.edu.upc.ice.track.platform.notifications.domain.repositories;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;

import java.time.Instant;
import java.util.Collection;
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
   * @param filter          severity, type and unit restrictions; never widens the recipient scope
   * @param page            zero-based page index
   * @param size            maximum number of notifications on the page
   * @return the notifications of that page and the total count
   */
  NotificationPage findActivePageByRecipientUserId(
      Long recipientUserId, boolean unreadOnly, NotificationFilter filter, int page, int size);

  /**
   * Counts a recipient's active notifications not yet read.
   *
   * @param recipientUserId the recipient user Id
   * @return the number of notifications neither read nor dismissed
   */
  long countActiveUnreadByRecipientUserId(Long recipientUserId);

  /**
   * Marks as read, in one statement, every active and unread notification of a recipient that
   * matches a filter.
   *
   * <p>Applies the same change as {@code Notification.markAsRead()} - read, with {@code readAt}
   * set - without loading the notifications one by one. A notification already read or dismissed
   * is left untouched.</p>
   *
   * @param recipientUserId the recipient user Id
   * @param filter          severity, type and unit restrictions; never widens the recipient scope
   * @param readAt          the instant to record as the read time
   * @return how many notifications changed from unread to read
   */
  long markActiveUnreadAsRead(Long recipientUserId, NotificationFilter filter, Instant readAt);

  /**
   * Marks as read, in one statement, the listed notifications that are addressed to a recipient,
   * active and unread. Any other identifier is ignored.
   *
   * @param recipientUserId the recipient user Id
   * @param notificationIds the notifications to mark
   * @param readAt          the instant to record as the read time
   * @return how many notifications changed from unread to read
   */
  long markActiveUnreadAsReadByIds(Long recipientUserId, Collection<Long> notificationIds, Instant readAt);
}
