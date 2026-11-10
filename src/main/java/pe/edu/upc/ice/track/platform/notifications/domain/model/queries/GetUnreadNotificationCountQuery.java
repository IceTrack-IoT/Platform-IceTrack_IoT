package pe.edu.upc.ice.track.platform.notifications.domain.model.queries;

/**
 * Query to count the notifications addressed to a recipient that are neither read nor dismissed,
 * the figure shown on the unread badge.
 *
 * @param recipientUserId the recipient, derived from the authenticated principal; required
 */
public record GetUnreadNotificationCountQuery(Long recipientUserId) {

  /**
   * Validates that a recipient was supplied.
   *
   * @throws IllegalArgumentException when the recipient is missing
   */
  public GetUnreadNotificationCountQuery {
    if (recipientUserId == null) {
      throw new IllegalArgumentException("recipientUserId must not be null");
    }
  }
}
