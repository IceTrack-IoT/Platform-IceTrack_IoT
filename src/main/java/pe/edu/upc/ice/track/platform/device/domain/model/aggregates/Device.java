package pe.edu.upc.ice.track.platform.device.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DevicePairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceRegisteredEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceStatusChangedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceUnpairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Aggregate root representing one physical IoT board.
 *
 * <p>The board, rather than its individual sensors or actuators, is the platform identity that
 * holds a credential and can be paired with one refrigeration unit. This aggregate never receives
 * an API key in plaintext: application services generate the secret and hand over only its
 * SHA-256 digest, which is what the edge authentication filter later looks the board up by.</p>
 *
 * <p>Lifecycle ({@link DeviceStatus}):</p>
 * <ul>
 *   <li>{@link #pairTo(Long, String)} links an {@code UNPAIRED} board to a unit with a fresh key
 *       digest, which makes it {@code PAIRED};</li>
 *   <li>{@link #revokeCredential()} drops the digest of a {@code PAIRED} board, which makes it
 *       {@code REVOKED}: the key stops matching immediately but the unit stays linked;</li>
 *   <li>{@link #rotateCredential(String)} installs a new digest on a {@code PAIRED} or
 *       {@code REVOKED} board, which makes it {@code PAIRED} again;</li>
 *   <li>{@link #unpair()} drops both the unit and the digest of a {@code PAIRED} or {@code REVOKED}
 *       board, which makes it {@code UNPAIRED} and lets the same board be paired again later.</li>
 * </ul>
 */
@Getter
public class Device extends AbstractDomainAggregateRoot<Device> {

  public static final int MAX_DEVICE_UID_LENGTH = 64;
  public static final int MAX_BOARD_MODEL_LENGTH = 64;
  public static final int MAX_FIRMWARE_VERSION_LENGTH = 64;
  /** Length of a hex-encoded SHA-256 digest. */
  public static final int API_KEY_DIGEST_LENGTH = 64;

  private static final Pattern HEX_DIGEST = Pattern.compile("[0-9a-f]{" + API_KEY_DIGEST_LENGTH + "}");

  private final Long deviceId;
  private final String deviceUid;
  private String boardModel;
  private String firmwareVersion;
  private String apiKeyDigest;
  private DeviceStatus status;
  private Long pairedEquipmentId;
  private Instant lastSeenAt;

  /** Creates a newly registered board: unpaired and without a credential. */
  public Device(Long deviceId, String deviceUid, String boardModel, String firmwareVersion) {
    this(deviceId, deviceUid, boardModel, firmwareVersion, null, DeviceStatus.UNPAIRED, null, null);
  }

  /**
   * Reconstitutes a board previously stored by the persistence adapter.
   *
   * @throws IllegalArgumentException when the stored combination of status, equipment and digest
   *                                  breaks the lifecycle invariants
   */
  public Device(Long deviceId, String deviceUid, String boardModel, String firmwareVersion,
                String apiKeyDigest, DeviceStatus status, Long pairedEquipmentId,
                Instant lastSeenAt) {
    this.deviceId = deviceId;
    this.deviceUid = requiredText(deviceUid, "Device uid", MAX_DEVICE_UID_LENGTH);
    this.boardModel = requiredText(boardModel, "Board model", MAX_BOARD_MODEL_LENGTH);
    this.firmwareVersion = optionalText(firmwareVersion, "Firmware version", MAX_FIRMWARE_VERSION_LENGTH);
    this.apiKeyDigest = apiKeyDigest == null ? null : validatedDigest(apiKeyDigest);
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.pairedEquipmentId = pairedEquipmentId;
    this.lastSeenAt = lastSeenAt;
    var consistent = switch (status) {
      case UNPAIRED -> pairedEquipmentId == null && this.apiKeyDigest == null;
      case PAIRED -> pairedEquipmentId != null && this.apiKeyDigest != null;
      case REVOKED -> pairedEquipmentId != null && this.apiKeyDigest == null;
    };
    if (!consistent) {
      throw new IllegalArgumentException(
          "Inconsistent device state: %s with equipment %s and %s API key digest".formatted(
              status, pairedEquipmentId, this.apiKeyDigest == null ? "no" : "an"));
    }
  }

  /**
   * Pairs this unpaired board to one refrigeration unit with a freshly issued credential.
   *
   * @param equipmentId  identifier of the unit; must be positive
   * @param apiKeyDigest hex SHA-256 digest of the new API key
   * @throws IllegalStateException    when the board is not {@code UNPAIRED}
   * @throws IllegalArgumentException when an argument is malformed
   */
  public void pairTo(Long equipmentId, String apiKeyDigest) {
    if (status != DeviceStatus.UNPAIRED) {
      throw new IllegalStateException("Device already paired");
    }
    if (equipmentId == null || equipmentId <= 0) {
      throw new IllegalArgumentException("Equipment id must be a positive number");
    }
    this.apiKeyDigest = validatedDigest(apiKeyDigest);
    this.pairedEquipmentId = equipmentId;
    this.status = DeviceStatus.PAIRED;
    this.lastSeenAt = null;
    registerDomainEvent(DevicePairedEvent.from(this));
    registerStatusChange(DeviceStatus.UNPAIRED, equipmentId);
  }

  /**
   * Replaces the credential of a linked board. On a {@code REVOKED} board this is the only way to
   * resume reporting, so the board becomes {@code PAIRED} again.
   *
   * @param newApiKeyDigest hex SHA-256 digest of the new API key
   * @throws IllegalStateException when the board is not linked to a unit
   */
  public void rotateCredential(String newApiKeyDigest) {
    if (status == DeviceStatus.UNPAIRED) {
      throw new IllegalStateException("An unpaired device has no credential to rotate");
    }
    var previousStatus = status;
    this.apiKeyDigest = validatedDigest(newApiKeyDigest);
    this.status = DeviceStatus.PAIRED;
    registerStatusChange(previousStatus, pairedEquipmentId);
  }

  /**
   * Withdraws the credential of a paired board while keeping it linked to its unit. Revoking an
   * already revoked board changes nothing.
   *
   * @throws IllegalStateException when the board is unpaired
   */
  public void revokeCredential() {
    if (status == DeviceStatus.UNPAIRED) {
      throw new IllegalStateException("An unpaired device has no credential to revoke");
    }
    var previousStatus = status;
    this.apiKeyDigest = null;
    this.status = DeviceStatus.REVOKED;
    if (previousStatus != DeviceStatus.REVOKED) {
      registerStatusChange(previousStatus, pairedEquipmentId);
    }
  }

  /**
   * Unlinks this board from its unit and invalidates its credential, so both the unit and the
   * board can be paired again.
   *
   * @throws IllegalStateException when the board is already unpaired
   */
  public void unpair() {
    if (status == DeviceStatus.UNPAIRED) {
      throw new IllegalStateException("Device is not paired with an equipment");
    }
    var previousEquipmentId = pairedEquipmentId;
    var previousStatus = status;
    this.pairedEquipmentId = null;
    this.apiKeyDigest = null;
    this.status = DeviceStatus.UNPAIRED;
    registerDomainEvent(DeviceUnpairedEvent.from(this, previousEquipmentId));
    registerStatusChange(previousStatus, previousEquipmentId);
  }

  /**
   * Registers a {@link DeviceStatusChangedEvent} for the change just applied. A board not persisted
   * yet raises nothing: nobody can be watching it.
   *
   * @param previousStatus the status before the change
   * @param equipmentId    the unit the board is, or was until this change, linked to
   */
  private void registerStatusChange(DeviceStatus previousStatus, Long equipmentId) {
    if (deviceId == null) {
      return;
    }
    registerDomainEvent(new DeviceStatusChangedEvent(
        deviceId, equipmentId, pairedEquipmentId, previousStatus, status, Instant.now()));
  }

  /**
   * Applies an operator-requested lifecycle transition.
   *
   * <p>{@code REVOKED} revokes the credential and {@code UNPAIRED} unpairs the board. Going back to
   * {@code PAIRED} always needs a new credential, so it is refused here and done by rotating the
   * API key instead.</p>
   *
   * @param targetStatus the status to move to; required
   * @throws IllegalStateException when the transition is not allowed
   */
  public void changeStatus(DeviceStatus targetStatus) {
    var target = Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    switch (target) {
      case REVOKED -> revokeCredential();
      case UNPAIRED -> unpair();
      case PAIRED -> {
        if (status != DeviceStatus.PAIRED) {
          throw new IllegalStateException(
              "A revoked device resumes only with a new credential; rotate its API key instead");
        }
      }
    }
  }

  /** Refreshes the hardware description reported when a board is paired again. */
  public void describeHardware(String boardModel, String firmwareVersion) {
    this.boardModel = requiredText(boardModel, "Board model", MAX_BOARD_MODEL_LENGTH);
    this.firmwareVersion = optionalText(firmwareVersion, "Firmware version", MAX_FIRMWARE_VERSION_LENGTH);
  }

  /** Registers the most recent successful authenticated contact from the board. */
  public void recordActivity(Instant seenAt) {
    if (status != DeviceStatus.PAIRED) {
      throw new IllegalStateException("Only a paired device can report activity");
    }
    lastSeenAt = Objects.requireNonNull(seenAt, "seenAt must not be null");
  }

  /** Tells whether this board may authenticate: paired, linked to a unit and holding a key. */
  public boolean isPaired() {
    return status == DeviceStatus.PAIRED && pairedEquipmentId != null && apiKeyDigest != null;
  }

  /** Tells whether this board currently occupies a unit, either paired or revoked. */
  public boolean isLinkedToEquipment() {
    return pairedEquipmentId != null;
  }

  /** Registers the event that needs the identity assigned by persistence. */
  public void onRegistered() {
    registerDomainEvent(DeviceRegisteredEvent.from(this));
  }

  private static String validatedDigest(String digest) {
    if (digest == null || !HEX_DIGEST.matcher(digest).matches()) {
      throw new IllegalArgumentException(
          "API key digest must be %d lowercase hexadecimal characters".formatted(API_KEY_DIGEST_LENGTH));
    }
    return digest;
  }

  private static String requiredText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be null or blank");
    }
    var trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(field + " must not exceed %d characters".formatted(maxLength));
    }
    return trimmed;
  }

  private static String optionalText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return requiredText(value, field, maxLength);
  }
}
