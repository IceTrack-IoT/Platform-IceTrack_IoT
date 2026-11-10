package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command to mark a selection of a recipient's notifications as read.
 *
 * <p>Identifiers that are not addressed to the recipient, or name a notification already read or
 * dismissed, are skipped silently rather than failing the whole command, so the outcome never tells
 * the caller which identifiers exist for somebody else.</p>
 *
 * @param recipientUserId recipient resolved from the authenticated principal; required
 * @param notificationIds identifiers of the notifications to mark; from 1 to {@value #MAX_IDS}
 *                        distinct values once duplicates and {@code null} entries are dropped
 */
public record MarkNotificationsAsReadCommand(Long recipientUserId, Set<Long> notificationIds) {

  /** Largest number of identifiers a single command may carry. */
  public static final int MAX_IDS = 100;

  /**
   * Validates the recipient and the selection.
   *
   * @throws IllegalArgumentException when the recipient is missing, or the selection is empty or
   *                                  larger than {@value #MAX_IDS}
   */
  public MarkNotificationsAsReadCommand {
    if (recipientUserId == null) {
      throw new IllegalArgumentException("recipientUserId must not be null");
    }
    if (notificationIds == null || notificationIds.isEmpty()) {
      throw new IllegalArgumentException("notification_ids must hold at least one identifier");
    }
    if (notificationIds.size() > MAX_IDS) {
      throw new IllegalArgumentException("notification_ids must not hold more than %d identifiers".formatted(MAX_IDS));
    }
    notificationIds = Set.copyOf(notificationIds);
  }

  /**
   * Builds the command from the identifiers as the client sent them, dropping {@code null} entries
   * and duplicates.
   *
   * @param recipientUserId the recipient
   * @param notificationIds the identifiers as received
   * @return the command
   * @throws IllegalArgumentException when nothing is left, or more than {@value #MAX_IDS} remain
   */
  public static MarkNotificationsAsReadCommand of(Long recipientUserId, List<Long> notificationIds) {
    var distinct = notificationIds == null
        ? Set.<Long>of()
        : notificationIds.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    return new MarkNotificationsAsReadCommand(recipientUserId, distinct);
  }
}
