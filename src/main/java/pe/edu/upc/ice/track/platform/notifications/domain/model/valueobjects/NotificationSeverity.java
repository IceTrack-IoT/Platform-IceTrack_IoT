package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

/**
 * How urgently a notification should be surfaced to its recipient.
 *
 * <p>{@code CRITICAL} is reserved for the brand's second-complementary red ({@code #FF5757})
 * in the Web/Mobile style guide; it must only be assigned to notifications that genuinely
 * require immediate attention.</p>
 */
public enum NotificationSeverity {
  INFO,
  WARNING,
  CRITICAL
}
