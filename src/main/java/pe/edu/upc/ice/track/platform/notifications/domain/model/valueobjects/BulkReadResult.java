package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

/**
 * Outcome of marking several of a recipient's notifications as read at once.
 *
 * @param updatedCount how many notifications changed from unread to read
 * @param unreadCount  how many of the recipient's notifications are still neither read nor
 *                     dismissed after the change
 */
public record BulkReadResult(long updatedCount, long unreadCount) {

  /**
   * Validates the counts.
   *
   * @throws IllegalArgumentException when a count is negative
   */
  public BulkReadResult {
    if (updatedCount < 0 || unreadCount < 0) {
      throw new IllegalArgumentException("Bulk read counts must not be negative");
    }
  }

  /**
   * Builds the outcome for a caller who has no inbox.
   *
   * @return an outcome reporting nothing updated and nothing unread
   */
  public static BulkReadResult nothing() {
    return new BulkReadResult(0L, 0L);
  }
}
