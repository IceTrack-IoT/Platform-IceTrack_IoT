package pe.edu.upc.ice.track.platform.notifications.infrastructure.push.fcm;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.push.PushGateway;

import java.util.*;

/**
 * {@link PushGateway} adapter backed by Firebase Cloud Messaging (HTTP v1 through the Admin SDK).
 *
 * <p>A token FCM reports as unregistered, or as belonging to another sender, is returned as invalid so
 * the caller forgets it. The Admin SDK does not always manage to read the error body FCM sends back,
 * and then reports no messaging error code; the HTTP status still tells an unregistered token apart,
 * because FCM answers it, and only it, with 404.</p>
 */
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
        var exception = result.getException();
        var error = exception.getMessagingErrorCode();
        var httpStatus = exception.getHttpResponse() == null ? null : exception.getHttpResponse().getStatusCode();
        if (isInvalidToken(error, httpStatus)) {
          invalid.add(batch.get(i));
        } else {
          log.warn("FCM rejected a push ({}, {}, HTTP {}): {}",
              error, exception.getErrorCode(), httpStatus, exception.getMessage());
        }
      }
      log.debug("FCM batch sent: {} ok, {} failed.", response.getSuccessCount(), response.getFailureCount());
    } catch (Exception ex) {
      log.error("FCM batch of {} token(s) failed.", batch.size(), ex);
    }
    return invalid;
  }

  /**
   * Tells whether FCM rejected a token for good, so it must be forgotten.
   *
   * @param error      the messaging error code the Admin SDK read from the response, or {@code null}
   *                   when it could not read one
   * @param httpStatus the HTTP status of FCM's response, or {@code null} when unknown
   * @return {@code true} for an unregistered token or one of another sender
   */
  static boolean isInvalidToken(MessagingErrorCode error, Integer httpStatus) {
    if (error != null) {
      return error == MessagingErrorCode.UNREGISTERED || error == MessagingErrorCode.SENDER_ID_MISMATCH;
    }
    return httpStatus != null && httpStatus == 404;
  }
}
