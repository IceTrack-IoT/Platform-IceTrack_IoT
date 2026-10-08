package pe.edu.upc.ice.track.platform.notifications.application.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.queryservices.NotificationQueryService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.interfaces.acl.NotificationsContextFacade;

/**
 * Default implementation of {@link NotificationsContextFacade}.
 *
 * <p>Translates the agnostic, primitives-only signature of the facade into a call against the
 * notifications application layer, and translates the result back into a primitive before
 * returning -- no notifications domain type ever crosses this boundary.</p>
 */
@Service
public class NotificationsContextFacadeImpl implements NotificationsContextFacade {

  private final NotificationQueryService notificationQueryService;

  public NotificationsContextFacadeImpl(NotificationQueryService notificationQueryService) {
    this.notificationQueryService = notificationQueryService;
  }

  @Override
  public int countUnreadNotificationsByRecipient(Long recipientUserId) {
    if (recipientUserId == null) {
      return 0;
    }
    return notificationQueryService.handle(new GetUnreadNotificationsByRecipientQuery(recipientUserId)).size();
  }
}
