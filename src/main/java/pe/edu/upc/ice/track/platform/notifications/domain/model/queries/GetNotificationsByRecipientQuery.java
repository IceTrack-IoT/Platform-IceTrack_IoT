package pe.edu.upc.ice.track.platform.notifications.domain.model.queries;

/**
 * Query to read one page of the active (not dismissed) notifications addressed to a recipient,
 * most recent first.
 *
 * <p>The page size is capped at {@value #MAX_PAGE_SIZE}: a larger value is lowered to the cap
 * rather than rejected.</p>
 *
 * @param recipientUserId the recipient, derived from the authenticated principal; required
 * @param unreadOnly      {@code true} to return only notifications not yet read
 * @param page            zero-based page index; must not be negative
 * @param size            page size; must be positive, capped at {@value #MAX_PAGE_SIZE}
 */
public record GetNotificationsByRecipientQuery(Long recipientUserId, boolean unreadOnly, int page, int size) {

  /** Page size used when the client does not supply one. */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /** Largest page size served. */
  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Validates the paging values and caps the page size.
   *
   * @throws IllegalArgumentException when the recipient is missing, the page index is negative or
   *                                  the page size is not positive
   */
  public GetNotificationsByRecipientQuery {
    if (recipientUserId == null) {
      throw new IllegalArgumentException("recipientUserId must not be null");
    }
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("size must be at least 1");
    }
    size = Math.min(size, MAX_PAGE_SIZE);
  }
}
