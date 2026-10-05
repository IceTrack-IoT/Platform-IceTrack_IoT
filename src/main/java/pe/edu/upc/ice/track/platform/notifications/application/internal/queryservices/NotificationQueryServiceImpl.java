package pe.edu.upc.ice.track.platform.notifications.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.queryservices.NotificationQueryService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;

import java.util.List;
import java.util.Optional;

/**
 *  Implementation of {@link NotificationQueryService} that uses a {@link NotificationRepository} to fetch notifications from the database.
 */
@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

  private final NotificationRepository notificationRepository;

  public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  /**
   *  Fetches a notification by its identifier.
   * @param query The query containing the notification Id
   * @return  An {@code Optional} containing the notification if found, or empty if not found.
   */
  @Override
  public Optional<Notification> handle(GetNotificationByIdQuery query) {
    return notificationRepository.findById(query.notificationId());
  }

  /**
   *  Fetches all notifications for a given recipient user.
   * @param query The query containing the recipient user Id
   * @return  A list of notifications for the given recipient user.
   */
  @Override
  public List<Notification> handle(GetNotificationsByRecipientQuery query) {
    return notificationRepository.findByRecipientUserId(query.recipientUserId());
  }

  /**
   *  Fetches all unread notifications for a given recipient user.
   * @param query The query containing the recipient user Id
   * @return  A list of unread notifications for the given recipient user.
   */
  @Override
  public List<Notification> handle(GetUnreadNotificationsByRecipientQuery query) {
    return notificationRepository.findByRecipientUserIdAndIsReadFalse(query.recipientUserId());
  }
}
