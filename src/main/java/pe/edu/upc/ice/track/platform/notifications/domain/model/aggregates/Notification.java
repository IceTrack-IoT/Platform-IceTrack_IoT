package pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Notification aggregate root of the {@code notifications} bounded context.
 *
 * <p>Deliberately simple: this context is a sink, not a source. A {@code Notification} is
 * always created as a translation of a fact that already happened somewhere else (an
 * {@code AlertRaisedIntegrationEvent} from Monitoring and Alerting Management today; a service
 * request update or a maintenance reminder in the future), never from a user-issued REST
 * request directly, and it never registers a domain event of its own because nothing downstream
 * needs to react to a notification being created, read or dismissed.</p>
 *
 * <p>{@code sourceAlertId} is kept as a bare identifier, never as a reference to Monitoring's
 * {@code Alert} aggregate -- this context only ever imports primitives across the boundary,
 * the same discipline every other ACL in this platform follows.</p>
 */
@Getter
public class Notification extends AbstractDomainAggregateRoot<Notification> {

  private Long id;
  private final Long recipientUserId;
  private EquipmentId equipmentId;
  private DeviceId deviceId;
  private AlertId sourceAlertId;
  private final String message;
  private final NotificationType type;
  private final NotificationSeverity severity;
  private boolean isRead;
  private LocalDateTime readAt;
  private LocalDateTime dismissedAt;

  /**
   *  Creates a new {@code Notification} from a command. The command is validated for nulls, but
   * @param command must be non-null and contain all required fields; otherwise, an exception is thrown.
   */
  public Notification(CreateNotificationCommand command) {
    this.recipientUserId = Objects.requireNonNull(command.recipientUserId(), "recipientUserId must not be null");
    this.message = Objects.requireNonNull(command.message(), "message must not be null");
    this.type = Objects.requireNonNull(command.type(), "type must not be null");
    this.severity = Objects.requireNonNull(command.severity(), "severity must not be null");
    this.equipmentId = new EquipmentId(command.equipmentId());
    this.deviceId = new DeviceId(command.deviceId());
    this.sourceAlertId = new AlertId(command.sourceAlertId());
    this.isRead = false;
  }

  /**
   * Rehydrates a {@code Notification} from persisted state, restoring every field exactly as
   * stored -- including {@code isRead}, {@code readAt} and {@code dismissedAt}, which the
   * creation constructor above cannot set. Used exclusively by
   * {@code NotificationPersistenceAssembler}; application and interface code must never call
   * this directly.
   */
  public static Notification rehydrate(
      Long id, Long recipientUserId, EquipmentId equipmentId, DeviceId deviceId, AlertId sourceAlertId,
      String message, NotificationType type, NotificationSeverity severity,
      boolean isRead, LocalDateTime readAt, LocalDateTime dismissedAt) {
    var notification = new Notification(
        new CreateNotificationCommand(recipientUserId, equipmentId, deviceId, sourceAlertId, message, type, severity));
    notification.id = id;
    notification.isRead = isRead;
    notification.readAt = readAt;
    notification.dismissedAt = dismissedAt;
    return notification;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  /** Marks the notification as read. Idempotent: reading an already-read notification is a no-op. */
  public void markAsRead() {
    if (isRead) {
      return;
    }
    this.isRead = true;
    this.readAt = LocalDateTime.now();
  }

  /** Dismisses the notification from the recipient's active inbox. Idempotent. */
  public void dismiss() {
    if (dismissedAt != null) {
      return;
    }
    this.dismissedAt = LocalDateTime.now();
  }
}
