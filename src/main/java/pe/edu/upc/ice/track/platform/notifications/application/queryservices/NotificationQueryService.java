package pe.edu.upc.ice.track.platform.notifications.application.queryservices;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationsByRecipientQuery;

import java.util.List;
import java.util.Optional;

/** Application service handling read operations on {@link Notification}. */
public interface NotificationQueryService {

  /**
   *  Fetch a single notification by its identifier.
   * @return  An {@link Optional} containing the notification if found, or empty if not found.
   */
  Optional<Notification> handle(GetNotificationByIdQuery query);

  /**
   *  Fetch all notifications for a given recipient user.
   * @param query The query containing the recipient user Id
   * @return  A list of notifications for the given recipient user.
   */
  List<Notification> handle(GetNotificationsByRecipientQuery query);

  /**
   *  Fetch all unread notifications for a given recipient user.
   * @param query The query containing the recipient user Id
   * @return  A list of unread notifications for the given recipient user.
   */
  List<Notification> handle(GetUnreadNotificationsByRecipientQuery query);
}
