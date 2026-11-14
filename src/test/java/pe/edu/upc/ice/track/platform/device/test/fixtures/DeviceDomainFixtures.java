package pe.edu.upc.ice.track.platform.device.test.fixtures;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

import java.time.Instant;

/**
 * Device-specific domain factories.
 *
 * <p>{@link Device} is a plain object (not a JPA entity), so tests build it through its public
 * constructors. Every factory returns a fresh, valid, internally consistent instance: an
 * {@code UNPAIRED} device carries neither equipment nor credential, a {@code PAIRED} one carries
 * both, and a {@code REVOKED} one keeps its equipment but has no credential.
 */
public final class DeviceDomainFixtures {

  private DeviceDomainFixtures() {
    throw new AssertionError("DeviceDomainFixtures is a utility class and must not be instantiated.");
  }


  public static final Long OWNER_ID = 42L;
  public static final Long USER_ID = 3L;
  public static final Long DEVICE_ID = 20L;
  public static final Long OTHER_DEVICE_ID = 21L;
  public static final Long EQUIPMENT_ID = 10L;
  public static final Long OTHER_EQUIPMENT_ID = 11L;
  public static final Long SITE_ID = 7L;
  public static final Long OTHER_SITE_ID = 8L;


  public static final String USERNAME = "owner@icetrack.pe";


  public static final String VALID_DEVICE_UID = "ESP32-DEV-0001";
  public static final String OTHER_DEVICE_UID = "ESP32-DEV-0002";
  public static final String VALID_BOARD_MODEL = "ESP32-S3";
  public static final String UPDATED_BOARD_MODEL = "ESP32-C6";
  public static final String VALID_FIRMWARE_VERSION = "1.2.0";
  public static final String UPDATED_FIRMWARE_VERSION = "1.3.0";


  public static final String VALID_DIGEST = "a".repeat(64);
  public static final String OTHER_DIGEST = "b".repeat(64);
  public static final String VALID_API_KEY = "it_dev_" + "c".repeat(64);
  public static final String OTHER_API_KEY = "it_dev_" + "d".repeat(64);

  public static final Instant LAST_SEEN_AT = Instant.parse("2026-01-01T10:00:00Z");

  public static Device unpairedDevice() {
    return deviceBuilder().build();
  }

  public static Device pairedDevice() {
    return deviceBuilder().paired().build();
  }

  public static Device revokedDevice() {
    return deviceBuilder().revoked().build();
  }

  public static DeviceBuilder deviceBuilder() {
    return new DeviceBuilder();
  }

  public static final class DeviceBuilder {
    private Long deviceId = DEVICE_ID;
    private String deviceUid = VALID_DEVICE_UID;
    private String boardModel = VALID_BOARD_MODEL;
    private String firmwareVersion = VALID_FIRMWARE_VERSION;
    private String apiKeyDigest;
    private DeviceStatus status = DeviceStatus.UNPAIRED;
    private Long pairedEquipmentId;
    private Instant lastSeenAt;

    public DeviceBuilder id(Long deviceId) {
      this.deviceId = deviceId;
      return this;
    }

    public DeviceBuilder uid(String deviceUid) {
      this.deviceUid = deviceUid;
      return this;
    }

    public DeviceBuilder boardModel(String boardModel) {
      this.boardModel = boardModel;
      return this;
    }

    public DeviceBuilder firmwareVersion(String firmwareVersion) {
      this.firmwareVersion = firmwareVersion;
      return this;
    }

    /** Pairs the device with {@link #EQUIPMENT_ID} and gives it {@link #VALID_DIGEST}. */
    public DeviceBuilder paired() {
      this.status = DeviceStatus.PAIRED;
      this.pairedEquipmentId = EQUIPMENT_ID;
      this.apiKeyDigest = VALID_DIGEST;
      return this;
    }

    /** Revokes the credential but keeps {@link #EQUIPMENT_ID}. */
    public DeviceBuilder revoked() {
      this.status = DeviceStatus.REVOKED;
      this.pairedEquipmentId = EQUIPMENT_ID;
      this.apiKeyDigest = null;
      return this;
    }

    public DeviceBuilder equipmentId(Long pairedEquipmentId) {
      this.pairedEquipmentId = pairedEquipmentId;
      return this;
    }

    public DeviceBuilder lastSeenAt(Instant lastSeenAt) {
      this.lastSeenAt = lastSeenAt;
      return this;
    }

    public Device build() {
      return new Device(
          deviceId, deviceUid, boardModel, firmwareVersion, apiKeyDigest, status, pairedEquipmentId, lastSeenAt);
    }
  }
}
