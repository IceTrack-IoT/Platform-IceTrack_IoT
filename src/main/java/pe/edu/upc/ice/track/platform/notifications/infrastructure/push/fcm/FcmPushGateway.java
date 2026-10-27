package pe.edu.upc.ice.track.platform.notifications.infrastructure.push.fcm;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.push.PushGateway;

import java.util.*;

/** {@link PushGateway} adapter backed by Firebase Cloud Messaging (HTTP v1 through the Admin SDK). */
@Slf4j
@Service
@ConditionalOnProperty(name = "notifications.push.provider", havingValue = "fcm")
public class FcmPushGateway implements PushGateway {

  /** FCM accepts at most 500 tokens per multicast request. */
  private static final int MAX_TOKENS_PER_BATCH = 500;

  private final FirebaseMessaging firebaseMessaging;

  public FcmPushGateway(FirebaseMessaging firebaseMessaging) {
    this.firebaseMessaging = firebaseMessaging;
  }

  @Override
  public Set<String> send(Collection<String> tokens, String title, String body, Map<String, String> data) {
    var invalid = new HashSet<String>();
    var all = new ArrayList<>(tokens);
    for (int from = 0; from < all.size(); from += MAX_TOKENS_PER_BATCH) {
      var batch = all.subList(from, Math.min(from + MAX_TOKENS_PER_BATCH, all.size()));
      invalid.addAll(sendBatch(batch, title, body, data));
    }
    return invalid;
  }

  private Set<String> sendBatch(List<String> batch, String title, String body, Map<String, String> data) {
    var invalid = new HashSet<String>();
    var message = MulticastMessage.builder()
        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
        .putAllData(data)
        .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
        .addAllTokens(batch)
        .build();
    try {
      var response = firebaseMessaging.sendEachForMulticast(message);
      var responses = response.getResponses();
      for (int i = 0; i < responses.size(); i++) {
        var result = responses.get(i);
        if (result.isSuccessful()) {
          continue;
        }
        var error = result.getException().getMessagingErrorCode();
        if (error == MessagingErrorCode.UNREGISTERED || error == MessagingErrorCode.SENDER_ID_MISMATCH) {
          invalid.add(batch.get(i));
        } else {
          log.warn("FCM rejected a push ({}): {}", error, result.getException().getMessage());
        }
      }
      log.debug("FCM batch sent: {} ok, {} failed.", response.getSuccessCount(), response.getFailureCount());
    } catch (Exception ex) {
      log.error("FCM batch of {} token(s) failed.", batch.size(), ex);
    }
    return invalid;
  }
}
