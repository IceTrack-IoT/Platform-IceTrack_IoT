package pe.edu.upc.ice.track.platform.notifications.application.queryservices;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationCountQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;

import java.util.Optional;

/** Application service handling read operations on {@link Notification}. */
public interface NotificationQueryService {

  /**
   *  Fetch a single notification by its identifier.
   * @return  An {@link Optional} containing the notification if found, or empty if not found.
   */
  Optional<Notification> handle(GetNotificationByIdQuery query);

  /**
   * Fetch one page of a recipient's active notifications, most recent first.
   *
   * @param query the recipient, the unread filter and the page to read
   * @return the notifications of that page and the paging metadata
   */
  NotificationPage handle(GetNotificationsByRecipientQuery query);

  /**
   * Count a recipient's notifications neither read nor dismissed.
   *
   * @param query the recipient
   * @return the unread count
   */
  long handle(GetUnreadNotificationCountQuery query);
}
