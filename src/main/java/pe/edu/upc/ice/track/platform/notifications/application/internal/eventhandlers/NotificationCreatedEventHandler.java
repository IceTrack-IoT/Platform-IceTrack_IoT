package pe.edu.upc.ice.track.platform.notifications.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.push.PushGateway;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.model.events.NotificationCreatedEvent;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.DeviceTokenRepository;

import java.util.HashMap;
import java.util.List;

/**
 * Pushes a freshly created notification to every device the recipient registered.
 *
 * <p>Runs on its own thread and never lets an exception escape: a provider outage must not
 * affect the notification, which is already persisted and visible in the inbox. Tokens the
 * provider reports as dead are deleted so they are not tried again.</p>
 */
@Slf4j
@Service
public class NotificationCreatedEventHandler {

  private final DeviceTokenRepository deviceTokenRepository;
  private final PushGateway pushGateway;

  public NotificationCreatedEventHandler(DeviceTokenRepository deviceTokenRepository, PushGateway pushGateway) {
    this.deviceTokenRepository = deviceTokenRepository;
    this.pushGateway = pushGateway;
  }

  @Async
  @EventListener
  public void on(NotificationCreatedEvent event) {
    try {
      List<String> tokens = deviceTokenRepository.findByRecipientUserId(event.recipientUserId()).stream()
          .map(DeviceToken::getToken)
          .toList();
      if (tokens.isEmpty()) {
        return;
      }

      var data = new HashMap<String, String>();
      data.put("notification_id", String.valueOf(event.notificationId()));
      data.put("type", event.type().name());
      data.put("severity", event.severity().name());
      if (event.equipmentId() != null) {
        data.put("equipment_id", String.valueOf(event.equipmentId()));
      }
      if (event.sourceAlertId() != null) {
        data.put("alert_id", String.valueOf(event.sourceAlertId()));
      }

      var invalidTokens = pushGateway.send(tokens, titleFor(event), event.message(), data);
      if (!invalidTokens.isEmpty()) {
        deviceTokenRepository.deleteByTokenIn(invalidTokens);
        log.info("Removed {} invalid push token(s) for recipient {}.", invalidTokens.size(), event.recipientUserId());
      }
    } catch (Exception ex) {
      log.error("Could not push notification {} to recipient {}.", event.notificationId(), event.recipientUserId(), ex);
    }
  }

  private String titleFor(NotificationCreatedEvent event) {
    return switch (event.severity()) {
      case CRITICAL -> "IceTrack - Critical alert";
      case WARNING -> "IceTrack - Warning";
      case INFO -> "IceTrack";
    };
  }
}
