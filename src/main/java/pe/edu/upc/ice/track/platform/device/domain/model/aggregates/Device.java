package pe.edu.upc.ice.track.platform.device.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceDecommissionedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DevicePairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceRegisteredEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root representing one physical IoT board.
 *
 * <p>The board, rather than its individual sensors or actuators, is the platform identity that
 * holds a credential and can be paired with one refrigeration unit. This aggregate deliberately
 * never receives an API key in plaintext: application services generate and hash that secret
 * before invoking {@link #replaceApiKeyHash(String)}. A freshly provisioned board does not have
 * a credential until its first pairing.</p>
 */
@Getter
public class Device extends AbstractDomainAggregateRoot<Device> {

  public static final int MAX_DEVICE_UID_LENGTH = 64;
  public static final int MAX_BOARD_MODEL_LENGTH = 64;
  public static final int MAX_FIRMWARE_VERSION_LENGTH = 64;
  public static final int MAX_API_KEY_HASH_LENGTH = 255;

  private final Long deviceId;
  private final String deviceUid;
  private String boardModel;
  private String firmwareVersion;
  private String apiKeyHash;
  private DeviceStatus status;
  private Long pairedEquipmentId;
  private LocalDateTime lastSeenAt;

  /** Creates a newly provisioned board, without an active credential. */
  public Device(Long deviceId, String deviceUid, String boardModel, String firmwareVersion) {
    this(deviceId, deviceUid, boardModel, firmwareVersion, null,
        DeviceStatus.PROVISIONED, null, null);
  }

  /** Reconstitutes a board previously stored by the persistence adapter. */
  public Device(Long deviceId, String deviceUid, String boardModel, String firmwareVersion,
                String apiKeyHash, DeviceStatus status, Long pairedEquipmentId,
                LocalDateTime lastSeenAt) {
    this.deviceId = deviceId;
    this.deviceUid = requiredText(deviceUid, "Device uid", MAX_DEVICE_UID_LENGTH);
    this.boardModel = requiredText(boardModel, "Board model", MAX_BOARD_MODEL_LENGTH);
    this.firmwareVersion = optionalText(firmwareVersion, "Firmware version", MAX_FIRMWARE_VERSION_LENGTH);
    this.apiKeyHash = optionalText(apiKeyHash, "API key hash", MAX_API_KEY_HASH_LENGTH);
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.pairedEquipmentId = pairedEquipmentId;
    this.lastSeenAt = lastSeenAt;
    if (status == DeviceStatus.ACTIVE && (pairedEquipmentId == null || apiKeyHash == null)) {
      throw new IllegalArgumentException("An active device must be paired and hold a credential");
    }
  }

  /** Pairs this board to one refrigeration unit and activates it. */
  public void pairTo(Long equipmentId) {
    if (status == DeviceStatus.REVOKED || status == DeviceStatus.DECOMMISSIONED) {
      throw new IllegalStateException("A revoked or decommissioned device cannot be paired");
    }
    if (equipmentId == null || equipmentId <= 0) {
      throw new IllegalArgumentException("Equipment id must be a positive number");
    }
    if (pairedEquipmentId != null) {
      throw new IllegalStateException("Device is already paired with an equipment");
    }
    if (apiKeyHash == null) {
      throw new IllegalStateException("A device must receive an API key before it can be paired");
    }
    pairedEquipmentId = equipmentId;
    if (status == DeviceStatus.PROVISIONED || status == DeviceStatus.SUSPENDED) {
      status = DeviceStatus.ACTIVE;
    }
    registerDomainEvent(DevicePairedEvent.from(this));
  }

  /** Removes the equipment relationship and returns the board to provisioned state. */
  public void unpair() {
    if (pairedEquipmentId == null) {
      throw new IllegalStateException("Device is not paired with an equipment");
    }
    if (status == DeviceStatus.REVOKED || status == DeviceStatus.DECOMMISSIONED) {
      throw new IllegalStateException("A revoked or decommissioned device cannot be unpaired");
    }
    pairedEquipmentId = null;
    status = DeviceStatus.PROVISIONED;
  }

  /** Replaces the protected verifier of the board API key. */
  public void replaceApiKeyHash(String newApiKeyHash) {
    if (status == DeviceStatus.REVOKED || status == DeviceStatus.DECOMMISSIONED) {
      throw new IllegalStateException("Credentials cannot be rotated for a revoked or decommissioned device");
    }
    apiKeyHash = requiredText(newApiKeyHash, "API key hash", MAX_API_KEY_HASH_LENGTH);
  }

  /** Applies an operator-requested lifecycle transition. */
  public void changeStatus(DeviceStatus targetStatus) {
    var target = Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    if (!status.canTransitionTo(target)) {
      throw new IllegalStateException(
          "Illegal device status transition from %s to %s; allowed targets are %s"
              .formatted(status, target, status.allowedTransitions()));
    }
    if (target == DeviceStatus.ACTIVE && (pairedEquipmentId == null || apiKeyHash == null)) {
      throw new IllegalStateException("An unpaired device without a credential cannot be activated");
    }
    status = target;
    if (target == DeviceStatus.DECOMMISSIONED) {
      registerDomainEvent(DeviceDecommissionedEvent.from(this));
    }
  }

  /** Registers the most recent successful authenticated contact from the board. */
  public void recordActivity(LocalDateTime seenAt) {
    if (status != DeviceStatus.ACTIVE) {
      throw new IllegalStateException("Only an active device can report activity");
    }
    lastSeenAt = Objects.requireNonNull(seenAt, "seenAt must not be null");
  }

  public boolean isActiveAndPaired() {
    return status == DeviceStatus.ACTIVE && pairedEquipmentId != null;
  }

  /** Registers the event that needs the identity assigned by persistence. */
  public void onRegistered() {
    registerDomainEvent(DeviceRegisteredEvent.from(this));
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
