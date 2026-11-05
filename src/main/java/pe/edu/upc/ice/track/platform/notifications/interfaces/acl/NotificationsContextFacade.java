package pe.edu.upc.ice.track.platform.notifications.interfaces.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.queryservices.NotificationQueryService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationsByRecipientQuery;

/**
 * ACL facade that exposes Notifications capabilities to other bounded contexts.
 *
 * <p>Every parameter and return type is a primitive -- no {@code Notification} aggregate or
 * repository is reachable from here. The primitives-only signature is translated into a call
 * against the notifications application layer, and the result back into a primitive before
 * returning. Deliberately minimal: today nothing in the platform calls into Notifications
 * synchronously, but the contract is defined up front so that a future consumer -- most likely a
 * Profiles and Preferences Management dashboard widget showing an unread count -- does not force
 * a breaking change to this facade.</p>
 */
@Service
public class NotificationsContextFacade {

  private final NotificationQueryService notificationQueryService;

  public NotificationsContextFacade(NotificationQueryService notificationQueryService) {
    this.notificationQueryService = notificationQueryService;
  }

  /** @return the number of unread notifications currently addressed to the recipient. */
  public int countUnreadNotificationsByRecipient(Long recipientUserId) {
    if (recipientUserId == null) {
      return 0;
    }
    return notificationQueryService.handle(new GetUnreadNotificationsByRecipientQuery(recipientUserId)).size();
  }
}
