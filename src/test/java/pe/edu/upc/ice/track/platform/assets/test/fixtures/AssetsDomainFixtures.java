package pe.edu.upc.ice.track.platform.assets.test.fixtures;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Assets-specific domain factories.
 *
 * <p>The {@code Equipment} and {@code Site} aggregates are plain objects (not JPA entities), so
 * tests build them through their public constructors and never need reflection to assign an id.
 * Every factory returns a fresh, valid instance built from the canonical constants below; use the
 * builders to vary only the attribute a given test cares about.
 */
public final class AssetsDomainFixtures {

  private AssetsDomainFixtures() {
    throw new AssertionError("AssetsDomainFixtures is a utility class and must not be instantiated.");
  }

  // ---------- Identifiers ----------

  public static final Long OWNER_ID = 42L;
  public static final Long OTHER_OWNER_ID = 99L;
  public static final Long USER_ID = 3L;
  public static final Long TECHNICIAN_ID = 5L;
  public static final Long SITE_ID = 7L;
  public static final Long OTHER_SITE_ID = 8L;
  public static final Long EQUIPMENT_ID = 10L;
  public static final Long OTHER_EQUIPMENT_ID = 11L;

  // ---------- Callers ----------

  public static final String USERNAME = "owner@icetrack.pe";
  public static final String TECHNICIAN_USERNAME = "technician@icetrack.pe";

  // ---------- Site ----------

  public static final String VALID_SITE_NAME = "Sede Central";
  public static final String VALID_CONTACT_NAME = "Maria Lopez";
  public static final String VALID_PHONE = "+51 987654321";
  public static final String VALID_ADDRESS = "Av. Primavera 123, Santiago de Surco, Lima, Peru";
  public static final String VALID_DISTRICT = "Santiago de Surco";
  public static final String VALID_PLACE_ID = "ChIJ8e8c9yLIBZERt0P0wP0xV5A";
  public static final String VALIDATION_TOKEN = "validation-token";

  /** A point inside Metropolitan Lima (and outside Callao). */
  public static final double LIMA_LATITUDE = -12.0931;
  public static final double LIMA_LONGITUDE = -77.0465;

  /** A point inside the Constitutional Province of Callao. */
  public static final double CALLAO_LATITUDE = -12.05;
  public static final double CALLAO_LONGITUDE = -77.12;

  /** A point far outside the service area (New York). */
  public static final double OUT_OF_COVERAGE_LATITUDE = 40.7128;
  public static final double OUT_OF_COVERAGE_LONGITUDE = -74.0060;

  /** About 11 meters from the Lima point: inside the 150 meter pin tolerance. */
  public static final double NEARBY_PIN_LATITUDE = -12.0932;
  public static final double NEARBY_PIN_LONGITUDE = -77.0466;

  /** About 1.1 kilometers from the Lima point: outside the 150 meter pin tolerance. */
  public static final double FAR_PIN_LATITUDE = -12.0831;
  public static final double FAR_PIN_LONGITUDE = -77.0465;

  // ---------- Equipment ----------

  public static final String VALID_UID = "ESP32-0001";
  public static final String VALID_EQUIPMENT_NAME = "Refrigerador 1";
  public static final Double VALID_MIN_CELSIUS = 2.0;
  public static final Double VALID_MAX_CELSIUS = 8.0;
  public static final Integer VALID_REMINDER_INTERVAL_DAYS = 90;

  // ---------- Value objects ----------

  public static GeoLocation geoLocation() {
    return new GeoLocation(LIMA_LATITUDE, LIMA_LONGITUDE, VALID_PLACE_ID);
  }

  public static VerifiedAddress verifiedAddress() {
    return new VerifiedAddress(
        VALID_ADDRESS, VALID_PLACE_ID, geoLocation(), VALID_DISTRICT, CoverageArea.LIMA_METROPOLITANA);
  }

  public static TemperatureThreshold temperatureThreshold() {
    return new TemperatureThreshold(VALID_MIN_CELSIUS, VALID_MAX_CELSIUS);
  }

  // ---------- Aggregates ----------

  /** An {@code ACTIVE}, offline unit with id {@link #EQUIPMENT_ID}, installed at {@link #SITE_ID}. */
  public static Equipment equipment() {
    return equipmentBuilder().build();
  }

  public static EquipmentBuilder equipmentBuilder() {
    return new EquipmentBuilder();
  }

  /** A located site with id {@link #SITE_ID}, owned by {@link #OWNER_ID}. */
  public static Site site() {
    return siteBuilder().build();
  }

  public static SiteBuilder siteBuilder() {
    return new SiteBuilder();
  }

  /**
   * Fluent builder over the reconstitution constructor of {@link Equipment}. Defaults describe an
   * {@code ACTIVE} unit that never reported.
   */
  public static final class EquipmentBuilder {
    private Long equipmentId = EQUIPMENT_ID;
    private Long siteId = SITE_ID;
    private String uid = VALID_UID;
    private String name = VALID_EQUIPMENT_NAME;
    private EquipmentType equipmentType = EquipmentType.REFRIGERATOR;
    private StatusEquipment status = StatusEquipment.ACTIVE;
    private boolean online = false;
    private TemperatureThreshold threshold = temperatureThreshold();
    private Integer reminderIntervalDays = VALID_REMINDER_INTERVAL_DAYS;
    private Instant lastReadingAt;
    private Double lastKnownTemperature;
    private Integer configVersion = Equipment.INITIAL_CONFIG_VERSION;
    private StatusEquipment statusBeforeMaintenance;
    private Instant lastReceivedAt;
    private boolean underMaintenance = false;
    private Long version = 0L;

    public EquipmentBuilder id(Long equipmentId) {
      this.equipmentId = equipmentId;
      return this;
    }

    public EquipmentBuilder siteId(Long siteId) {
      this.siteId = siteId;
      return this;
    }

    public EquipmentBuilder uid(String uid) {
      this.uid = uid;
      return this;
    }

    public EquipmentBuilder name(String name) {
      this.name = name;
      return this;
    }

    public EquipmentBuilder type(EquipmentType equipmentType) {
      this.equipmentType = equipmentType;
      return this;
    }

    public EquipmentBuilder status(StatusEquipment status) {
      this.status = status;
      return this;
    }

    public EquipmentBuilder online(boolean online) {
      this.online = online;
      return this;
    }

    public EquipmentBuilder threshold(TemperatureThreshold threshold) {
      this.threshold = threshold;
      return this;
    }

    public EquipmentBuilder reminderIntervalDays(Integer reminderIntervalDays) {
      this.reminderIntervalDays = reminderIntervalDays;
      return this;
    }

    public EquipmentBuilder lastReading(Instant lastReadingAt, Double lastKnownTemperature) {
      this.lastReadingAt = lastReadingAt;
      this.lastKnownTemperature = lastKnownTemperature;
      return this;
    }

    public EquipmentBuilder configVersion(Integer configVersion) {
      this.configVersion = configVersion;
      return this;
    }

    public EquipmentBuilder statusBeforeMaintenance(StatusEquipment statusBeforeMaintenance) {
      this.statusBeforeMaintenance = statusBeforeMaintenance;
      return this;
    }

    public EquipmentBuilder lastReceivedAt(Instant lastReceivedAt) {
      this.lastReceivedAt = lastReceivedAt;
      return this;
    }

    public EquipmentBuilder underMaintenance(boolean underMaintenance) {
      this.underMaintenance = underMaintenance;
      return this;
    }

    public EquipmentBuilder version(Long version) {
      this.version = version;
      return this;
    }

    public Equipment build() {
      return new Equipment(
          equipmentId, siteId, uid, name, equipmentType, status, online, threshold, reminderIntervalDays,
          lastReadingAt, lastKnownTemperature, configVersion, statusBeforeMaintenance, lastReceivedAt,
          underMaintenance, version);
    }
  }

  /**
   * Fluent builder over the reconstitution constructor of {@link Site}. Defaults describe a located
   * ({@code RESOLVED}) site with no equipment.
   */
  public static final class SiteBuilder {
    private Long siteId = SITE_ID;
    private Long ownerId = OWNER_ID;
    private Long version = 1L;
    private String name = VALID_SITE_NAME;
    private Address address = new Address(VALID_ADDRESS);
    private String contactName = VALID_CONTACT_NAME;
    private SiteContactPhone phone = new SiteContactPhone(VALID_PHONE);
    private Integer equipmentCount = 0;
    private GeocodingStatus geocodingStatus = GeocodingStatus.RESOLVED;
    private GeoLocation location = geoLocation();
    private GeocodingFailureReason failureReason;

    public SiteBuilder id(Long siteId) {
      this.siteId = siteId;
      return this;
    }

    public SiteBuilder ownerId(Long ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public SiteBuilder version(Long version) {
      this.version = version;
      return this;
    }

    public SiteBuilder name(String name) {
      this.name = name;
      return this;
    }

    public SiteBuilder address(String address) {
      this.address = new Address(address);
      return this;
    }

    public SiteBuilder equipmentCount(Integer equipmentCount) {
      this.equipmentCount = equipmentCount;
      return this;
    }

    public SiteBuilder location(GeoLocation location) {
      this.location = location;
      return this;
    }

    /** Leaves the site without a location, in the given (non resolved) geocoding state. */
    public SiteBuilder unlocated(GeocodingStatus status, GeocodingFailureReason failureReason) {
      this.geocodingStatus = status;
      this.failureReason = failureReason;
      this.location = null;
      return this;
    }

    public Site build() {
      return new Site(
          siteId, ownerId, version, name, address, contactName, phone, equipmentCount, geocodingStatus,
          location, failureReason);
    }
  }
}
