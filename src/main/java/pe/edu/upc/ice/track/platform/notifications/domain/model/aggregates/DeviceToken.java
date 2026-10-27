package pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An FCM registration token bound to the recipient that owns the device.
 *
 * <p>The token is the natural identity: FCM issues one per app instance, so registering a token
 * that already exists never creates a second row. If the same device is later used by another
 * account (shared phone, re-login) the token is simply handed over to the new recipient with
 * {@link #reassignTo}, so a push can never reach the previous user.</p>
 */
@Getter
public class DeviceToken {

  private Long id;
  private Long recipientUserId;
  private final String token;
  private DevicePlatform platform;
  private LocalDateTime lastSeenAt;

  public DeviceToken(Long recipientUserId, String token, DevicePlatform platform) {
    this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("token must not be blank");
    }
    this.token = token;
    this.platform = Objects.requireNonNull(platform, "platform must not be null");
    this.lastSeenAt = LocalDateTime.now();
  }

  /** Rehydrates a token from persisted state. Used only by the persistence assembler. */
  public static DeviceToken rehydrate(
      Long id, Long recipientUserId, String token, DevicePlatform platform, LocalDateTime lastSeenAt) {
    var deviceToken = new DeviceToken(recipientUserId, token, platform);
    deviceToken.id = id;
    deviceToken.lastSeenAt = lastSeenAt;
    return deviceToken;
  }

  /** Hands the token to a recipient and records that the device was just seen. */
  public void reassignTo(Long newRecipientUserId, DevicePlatform newPlatform) {
    this.recipientUserId = Objects.requireNonNull(newRecipientUserId, "recipientUserId must not be null");
    this.platform = Objects.requireNonNull(newPlatform, "platform must not be null");
    this.lastSeenAt = LocalDateTime.now();
  }

  public boolean belongsTo(Long candidateRecipientUserId) {
    return recipientUserId.equals(candidateRecipientUserId);
  }
}
