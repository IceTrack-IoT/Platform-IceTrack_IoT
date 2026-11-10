package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;

/**
 * Command to mark every active, unread notification of a recipient as read, optionally only those
 * matching a filter.
 *
 * <p>Idempotent: a second run finds nothing left to update.</p>
 *
 * @param recipientUserId recipient resolved from the authenticated principal; required
 * @param filter          restricts which notifications are marked; {@code null} marks them all
 */
public record MarkAllNotificationsAsReadCommand(Long recipientUserId, NotificationFilter filter) {

  /**
   * Validates the recipient and normalizes the filter.
   *
   * @throws IllegalArgumentException when the recipient is missing
   */
  public MarkAllNotificationsAsReadCommand {
    if (recipientUserId == null) {
      throw new IllegalArgumentException("recipientUserId must not be null");
    }
    filter = filter == null ? NotificationFilter.NONE : filter;
  }
}
