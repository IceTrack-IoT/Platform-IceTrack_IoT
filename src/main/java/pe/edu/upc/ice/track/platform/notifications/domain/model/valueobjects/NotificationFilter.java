package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import java.util.Set;

/**
 * Optional restrictions on which of a recipient's notifications an inbox read or a bulk read
 * transition applies to.
 *
 * <p>Every restriction only narrows the recipient's own notifications: the recipient itself is
 * never part of the filter, so no combination of values can reach another recipient's inbox. A
 * unit of another owner simply matches nothing.</p>
 *
 * @param severities  restrict to these severities; {@code null} or empty does not restrict
 * @param types       restrict to these kinds; {@code null} or empty does not restrict
 * @param equipmentId restrict to the notifications about one unit; {@code null} does not restrict
 */
public record NotificationFilter(Set<NotificationSeverity> severities, Set<NotificationType> types, Long equipmentId) {

  /** A filter that restricts nothing. */
  public static final NotificationFilter NONE = new NotificationFilter(Set.of(), Set.of(), null);

  /** Normalizes missing sets to empty, immutable ones. */
  public NotificationFilter {
    severities = severities == null ? Set.of() : Set.copyOf(severities);
    types = types == null ? Set.of() : Set.copyOf(types);
  }
}
