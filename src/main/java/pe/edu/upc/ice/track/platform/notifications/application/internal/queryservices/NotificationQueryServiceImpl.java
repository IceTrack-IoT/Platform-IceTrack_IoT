package pe.edu.upc.ice.track.platform.notifications.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.notifications.application.queryservices.NotificationQueryService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationCountQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;

import java.util.Optional;

/**
 *  Implementation of {@link NotificationQueryService} that uses a {@link NotificationRepository} to fetch notifications from the database.
 *
 * <p>Listings and counts only consider active notifications: a dismissed notification is hidden
 * from the inbox and from the unread badge, but it is kept.</p>
 */
@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

  private final NotificationRepository notificationRepository;

  public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  /**
   *  Fetches one of the caller's notifications by its identifier.
   * @param query The query containing the notification ID and the caller's recipient identifier
   * @return  An {@code Optional} containing the notification if found and addressed to the caller,
   *          or empty otherwise.
   */
  @Override
  public Optional<Notification> handle(GetNotificationByIdQuery query) {
    return notificationRepository.findById(query.notificationId())
        .filter(notification -> notification.isAddressedTo(query.recipientUserId()));
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public NotificationPage handle(GetNotificationsByRecipientQuery query) {
    return notificationRepository.findActivePageByRecipientUserId(
        query.recipientUserId(), query.unreadOnly(), query.filter(), query.page(), query.size());
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public long handle(GetUnreadNotificationCountQuery query) {
    return notificationRepository.countActiveUnreadByRecipientUserId(query.recipientUserId());
  }
}
