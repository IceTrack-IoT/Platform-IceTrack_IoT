package pe.edu.upc.ice.track.platform.notifications.interfaces.acl;

/**
 * ACL facade that exposes Notifications capabilities to other bounded contexts.
 *
 * <p>Every parameter and return type is a primitive -- no {@code Notification} aggregate or
 * repository is reachable from here. Deliberately minimal: today nothing in the platform calls
 * into Notifications synchronously, but the contract is defined up front so that a future
 * consumer -- most likely a Profiles and Preferences Management dashboard widget showing an
 * unread count -- does not force a breaking change to this interface.</p>
 */
public interface NotificationsContextFacade {

  /** @return the number of unread notifications currently addressed to the recipient. */
  int countUnreadNotificationsByRecipient(Long recipientUserId);
}
