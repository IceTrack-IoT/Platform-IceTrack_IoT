package pe.edu.upc.ice.track.platform.monitoring.infrastructure.notifications.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.NotificationCreatedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.UnreadCountChangedStreamResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.NotificationCreatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.UnreadNotificationCountChangedIntegrationEvent;

/**
 * Inbound infrastructure adapter that pushes the notification events of the Notifications context to
 * the owner's event stream, so the bell and the notification center update without polling.
 *
 * <p>Runs once the change has committed, on the {@code streamTaskExecutor} pool. The recipient named
 * by the event is the only stream written to; only owners open streams, so an event addressed to a
 * technician reaches nobody. Failures are logged and swallowed: the change is stored and the client
 * reads it over the REST API.</p>
 */
@Component
@Slf4j
public class NotificationStreamEventListener {

  /** SSE event name of a stored notification. */
  public static final String NOTIFICATION_CREATED_EVENT = "notification-created";

  /** SSE event name of a changed unread count. */
  public static final String UNREAD_COUNT_CHANGED_EVENT = "unread-count-changed";

  private final TelemetryStreamRegistry telemetryStreamRegistry;

  public NotificationStreamEventListener(TelemetryStreamRegistry telemetryStreamRegistry) {
    this.telemetryStreamRegistry = telemetryStreamRegistry;
  }

  /**
   * @param event the notification just stored
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(NotificationCreatedIntegrationEvent event) {
    if (!telemetryStreamRegistry.isFollowed(event.recipientProfileId())) {
      return;
    }
    try {
      telemetryStreamRegistry.publish(event.recipientProfileId(), NOTIFICATION_CREATED_EVENT,
          new NotificationCreatedStreamResource(
              event.notificationId(),
              event.type(),
              event.severity(),
              event.equipmentId(),
              event.sourceAlertId(),
              event.createdAt(),
              event.unreadCount()));
    } catch (RuntimeException e) {
      log.warn("Could not stream notification {}: {}", event.notificationId(), e.getMessage(), e);
    }
  }

  /**
   * @param event the recipient's new unread count
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(UnreadNotificationCountChangedIntegrationEvent event) {
    if (!telemetryStreamRegistry.isFollowed(event.recipientProfileId())) {
      return;
    }
    try {
      telemetryStreamRegistry.publish(event.recipientProfileId(), UNREAD_COUNT_CHANGED_EVENT,
          new UnreadCountChangedStreamResource(event.unreadCount(), event.changedAt()));
    } catch (RuntimeException e) {
      log.warn("Could not stream the unread count of recipient {}: {}", event.recipientProfileId(), e.getMessage(), e);
    }
  }
}
