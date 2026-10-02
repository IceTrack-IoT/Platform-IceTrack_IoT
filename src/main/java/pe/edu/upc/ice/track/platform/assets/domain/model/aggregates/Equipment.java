package pe.edu.upc.ice.track.platform.assets.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.TemperatureThreshold;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Equipment aggregate root of the {@code assets} bounded context.
 *
 * <p>A single refrigeration unit installed at a {@link Site}. It is the equipment catalogue entry
 * that Monitoring and Alerting and Device Management both identify by, and the object that owns
 * the two settings this context is authoritative for: the {@link TemperatureThreshold} of the
 * unit and its preventive maintenance interval.</p>
 *
 * <p>A unit belongs to exactly one site, and therefore to exactly one owner. The owner is never
 * held here: it is reached through the site's {@code ownerId}, which is why the repository has to
 * join {@code equipments} with {@code sites} to answer an owner-scoped query.</p>
 *
 * <p>{@code online}, {@code lastReadingAt} and {@code lastKnownTemperature} are the equipment's
 * view of the telemetry it produces. They are persisted and exposed here, but this context does
 * not ingest readings: the Monitoring and Alerting context writes them when it evaluates a
 * reading, which is why they are only reachable through {@link #recordReading}.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code EquipmentPersistenceEntity}.</p>
 */
@Getter
public class Equipment extends AbstractDomainAggregateRoot<Equipment> {

  /**
   * Maximum number of characters the unit's own identifier may hold.
   */
  public static final int MAX_UID_LENGTH = 30;

  /**
   * Maximum number of characters the unit name may hold.
   */
  public static final int MAX_NAME_LENGTH = 30;

  /**
   * Largest preventive maintenance interval, in days, this context accepts. One year is already
   * longer than any refrigeration unit is serviced for.
   */
  public static final int MAX_REMINDER_INTERVAL_DAYS = 365;

  private final Long equipmentId;
  private final Long siteId;
  private String uid;
  private String name;
  private EquipmentType equipmentType;
  private StatusEquipment status;
  private boolean online;
  private TemperatureThreshold temperatureThreshold;
  private Integer reminderIntervalDays;
  private LocalDateTime lastReadingAt;
  private Double lastKnownTemperature;

  /**
   * Registers a new equipment unit at a site.
   *
   * <p>The unit starts {@link StatusEquipment#AVAILABLE} and disconnected: a unit is catalogued
   * before a device is ever paired with it, so claiming it is already running or already online
   * would be a lie the dashboard would then report against.</p>
   *
   * @param equipmentId         the persistence identity, or {@code null} when not yet persisted
   * @param siteId              identifier of the site the unit is installed at; required
   * @param uid                 the unit's own identifier, unique across the platform; required
   * @param name                the name assigned to the unit; required
   * @param equipmentType       the kind of refrigeration unit; required
   * @param temperatureThreshold the acceptable temperature band; required
   * @param reminderIntervalDays the preventive maintenance interval in days; required and
   *                            strictly positive
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public Equipment(Long equipmentId, Long siteId, String uid, String name, EquipmentType equipmentType,
                   TemperatureThreshold temperatureThreshold, Integer reminderIntervalDays) {
    this.equipmentId = equipmentId;
    this.siteId = Objects.requireNonNull(siteId, "siteId must not be null");
    this.uid = validatedUid(uid);
    this.name = validatedName(name);
    this.equipmentType = Objects.requireNonNull(equipmentType, "equipmentType must not be null");
    this.temperatureThreshold = Objects.requireNonNull(
        temperatureThreshold, "temperatureThreshold must not be null");
    this.reminderIntervalDays = validatedReminderIntervalDays(reminderIntervalDays);
    this.status = StatusEquipment.AVAILABLE;
    this.online = false;
  }

  /**
   * Reconstitutes an equipment unit from persistence, including the operational fields this
   * context does not own.
   *
   * @param equipmentId           the persistence identity
   * @param siteId                identifier of the site the unit is installed at
   * @param uid                   the unit's own identifier
   * @param name                  the name assigned to the unit
   * @param equipmentType         the kind of refrigeration unit
   * @param status                the current operational status
   * @param online                whether the unit is connected
   * @param temperatureThreshold  the acceptable temperature band
   * @param reminderIntervalDays  the preventive maintenance interval in days
   * @param lastReadingAt         when the last reading was received, may be {@code null}
   * @param lastKnownTemperature  the last temperature known, may be {@code null}
   */
  public Equipment(Long equipmentId, Long siteId, String uid, String name, EquipmentType equipmentType,
                   StatusEquipment status, boolean online, TemperatureThreshold temperatureThreshold,
                   Integer reminderIntervalDays, LocalDateTime lastReadingAt, Double lastKnownTemperature) {
    this.equipmentId = equipmentId;
    this.siteId = Objects.requireNonNull(siteId, "siteId must not be null");
    this.uid = validatedUid(uid);
    this.name = validatedName(name);
    this.equipmentType = Objects.requireNonNull(equipmentType, "equipmentType must not be null");
    this.temperatureThreshold = Objects.requireNonNull(
        temperatureThreshold, "temperatureThreshold must not be null");
    this.reminderIntervalDays = validatedReminderIntervalDays(reminderIntervalDays);
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.online = online;
    this.lastReadingAt = lastReadingAt;
    this.lastKnownTemperature = lastKnownTemperature;
  }

  /**
   * Replaces the descriptive data and the preventive maintenance interval of this unit.
   *
   * <p>The {@code uid}, the site and the threshold are excluded on purpose. The uid is the unit's
   * identity as printed on the device and must stay stable across a rename; the site is fixed
   * because moving a unit is not a rename; the threshold has its own command so that changing it
   * always publishes its own domain event.</p>
   *
   * @param name                 the new unit name; required
   * @param equipmentType        the new kind of refrigeration unit; required
   * @param reminderIntervalDays the new preventive maintenance interval in days; required
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public void updateInfo(String name, EquipmentType equipmentType, Integer reminderIntervalDays) {
    this.name = validatedName(name);
    this.equipmentType = Objects.requireNonNull(equipmentType, "equipmentType must not be null");
    this.reminderIntervalDays = validatedReminderIntervalDays(reminderIntervalDays);
  }

  /**
   * Replaces the acceptable temperature band of this unit.
   *
   * <p>Constructing the {@link TemperatureThreshold} is what enforces {@code minCelsius <
   * maxCelsius}, so an invalid band never reaches the aggregate and no event is registered.</p>
   *
   * @param temperatureThreshold the new acceptable temperature band; required
   * @throws IllegalArgumentException when {@code minCelsius} is not strictly below {@code maxCelsius}
   */
  public void changeThreshold(TemperatureThreshold temperatureThreshold) {
    var validated = Objects.requireNonNull(temperatureThreshold, "temperatureThreshold must not be null");
    this.temperatureThreshold = validated;
    registerDomainEvent(TemperatureThresholdUpdatedEvent.from(this));
  }

  /**
   * Moves this unit to a new operational status.
   *
   * <p>The transition is checked against the matrix carried by {@link StatusEquipment}. A
   * forbidden move leaves the unit untouched and is reported as a conflict, because it means the
   * caller asked for something the domain does not allow rather than something that happens to
   * fail.</p>
   *
   * @param targetStatus the status to move to; required
   * @throws IllegalArgumentException when the target status is missing
   * @throws IllegalStateException    when the transition is not part of the matrix
   */
  public void changeStatus(StatusEquipment targetStatus) {
    var target = Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    if (!this.status.canTransitionTo(target)) {
      throw new IllegalStateException(
          "Illegal equipment status transition from %s to %s; allowed targets are %s"
              .formatted(this.status, target, this.status.allowedTransitions()));
    }
    this.status = target;
    // A unit that is not powered cannot be connected; keeping the two consistent stops the
    // dashboard from showing an OFF unit as still reporting.
    if (target == StatusEquipment.OFF || target == StatusEquipment.AVAILABLE) {
      this.online = false;
    }
  }

  /**
   * Records that a reading was received from this unit.
   *
   * <p>Invoked by the Monitoring and Alerting context, which is the only writer of these fields.
   * This context stores and exposes them; it does not evaluate them.</p>
   *
   * @param temperature the temperature that was read
   * @param readAt      when the reading was taken; required
   */
  public void recordReading(Double temperature, LocalDateTime readAt) {
    this.lastKnownTemperature = temperature;
    this.lastReadingAt = Objects.requireNonNull(readAt, "readAt must not be null");
  }

  /**
   * Tells whether this unit is installed at the given site.
   *
   * @param candidateSiteId the site identifier to test
   * @return {@code true} when the identifiers match
   */
  public boolean isInstalledAt(Long candidateSiteId) {
    return siteId.equals(candidateSiteId);
  }

  /**
   * Signals that this unit has just been registered and persisted.
   *
   * <p>Called by the repository adapter once the persistence identity is available, since the
   * registration event carries it.</p>
   */
  public void onRegistered() {
    registerDomainEvent(EquipmentRegisteredEvent.from(this));
  }

  private static String validatedUid(String uid) {
    if (uid == null || uid.isBlank()) {
      throw new IllegalArgumentException("Equipment uid must not be null or blank");
    }
    var trimmed = uid.trim();
    if (trimmed.length() > MAX_UID_LENGTH) {
      throw new IllegalArgumentException("Equipment uid must not exceed %d characters".formatted(MAX_UID_LENGTH));
    }
    return trimmed;
  }

  private static String validatedName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Equipment name must not be null or blank");
    }
    var trimmed = name.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("Equipment name must not exceed %d characters".formatted(MAX_NAME_LENGTH));
    }
    return trimmed;
  }

  private static Integer validatedReminderIntervalDays(Integer reminderIntervalDays) {
    if (reminderIntervalDays == null) {
      throw new IllegalArgumentException("Reminder interval days must not be null");
    }
    if (reminderIntervalDays <= 0) {
      throw new IllegalArgumentException("Reminder interval days must be a positive number of days");
    }
    if (reminderIntervalDays > MAX_REMINDER_INTERVAL_DAYS) {
      throw new IllegalArgumentException(
          "Reminder interval days must not exceed %d days".formatted(MAX_REMINDER_INTERVAL_DAYS));
    }
    return reminderIntervalDays;
  }
}