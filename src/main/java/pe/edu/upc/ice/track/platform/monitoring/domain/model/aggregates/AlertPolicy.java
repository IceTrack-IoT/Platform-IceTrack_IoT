package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertPolicyUpdatedEvent;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * AlertPolicy aggregate root of the {@code monitoring} bounded context.
 *
 * <p>A policy with {@code equipmentId == null} is the platform-wide default, evaluated whenever
 * no equipment-specific policy is active for the equipment being evaluated. Kept as its own
 * aggregate — rather than embedded on the Asset Management {@code Equipment} — because it is
 * queried and updated independently of the equipment catalogue, and belongs to the context that
 * actually consumes it when evaluating a reading.</p>
 *
 * <p>{@code hysteresisMargin} is a {@link Temperature} Value Object, not a raw {@code Double} —
 * it is a Celsius value just like {@code SensorReading.maxTemperature}, so it earns the same
 * physical-plausibility guarantee. {@code sustainedExcursionMinutes} and
 * {@code missedSyncWindowsForOffline} stay as plain {@code Integer}: they are counts of minutes
 * and sync windows, not measured physical quantities, so there is no domain rule to enforce
 * beyond "non-negative", which the REST layer already validates at the boundary.</p>
 */
@Getter
public class AlertPolicy extends AbstractDomainAggregateRoot<AlertPolicy> {

  /** Minutes an excursion must last before an alert opens. */
  public static final int DEFAULT_SUSTAINED_EXCURSION_MINUTES = 2;

  /** Re-entry margin in Celsius; 1.0 matches the 1 °C resolution of the DHT11 sensor. */
  public static final double DEFAULT_HYSTERESIS_MARGIN_CELSIUS = 1.0;

  /** Missed synchronization windows before a device is considered offline. */
  public static final int DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE = 3;

  /**
   * Length of one synchronization window: a monitored unit's gateway delivers at least one reading
   * batch per window.
   */
  public static final Duration SYNC_WINDOW = Duration.ofSeconds(60);

  private Long id;
  private EquipmentId equipmentId;
  private Integer sustainedExcursionMinutes;
  private Temperature hysteresisMargin;
  private Integer missedSyncWindowsForOffline;
  private boolean active;

  public AlertPolicy(EquipmentId equipmentId, Integer sustainedExcursionMinutes,
                     Double hysteresisMarginCelsius, Integer missedSyncWindowsForOffline) {
    this.equipmentId = equipmentId;
    this.sustainedExcursionMinutes = sustainedExcursionMinutes;
    this.hysteresisMargin = new Temperature(hysteresisMarginCelsius);
    this.missedSyncWindowsForOffline = missedSyncWindowsForOffline;
    this.active = true;
  }

  /**
   * Builds the platform-wide default policy, which applies to every unit without a policy of its
   * own.
   *
   * @return a new default policy, not yet persisted
   */
  public static AlertPolicy platformDefault() {
    return new AlertPolicy(null, DEFAULT_SUSTAINED_EXCURSION_MINUTES,
        DEFAULT_HYSTERESIS_MARGIN_CELSIUS, DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE);
  }

  /** @return {@code true} for the platform-wide default, which is bound to no equipment. */
  public boolean isPlatformDefault() {
    return equipmentId == null;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  /**
   * Replaces the settings of this policy.
   *
   * <p>For a policy bound to a unit, registers an {@link AlertPolicyUpdatedEvent}: the unit's edge
   * gateway enforces these settings and must download them again. The platform default is not
   * writable over REST and is bound to no unit, so it registers nothing.</p>
   *
   * @param sustainedExcursionMinutes   minutes an excursion must last before it is raised
   * @param hysteresisMarginCelsius     margin the temperature must recover by before an excursion closes
   * @param missedSyncWindowsForOffline missed synchronization windows before a device counts as offline
   */
  public void updatePolicy(Integer sustainedExcursionMinutes, Double hysteresisMarginCelsius,
                            Integer missedSyncWindowsForOffline) {
    this.sustainedExcursionMinutes = sustainedExcursionMinutes;
    this.hysteresisMargin = new Temperature(hysteresisMarginCelsius);
    this.missedSyncWindowsForOffline = missedSyncWindowsForOffline;
    if (!isPlatformDefault()) {
      registerDomainEvent(new AlertPolicyUpdatedEvent(
          equipmentId.equipmentId(), sustainedExcursionMinutes, hysteresisMarginCelsius, missedSyncWindowsForOffline));
    }
  }

  public void deactivate() {
    this.active = false;
  }

  /**
   * How long a unit may stay silent before it counts as offline: {@link #SYNC_WINDOW} times
   * {@code missedSyncWindowsForOffline}. A missing or non-positive count falls back to
   * {@value #DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE}.
   *
   * @return the silence that makes a unit offline
   */
  public Duration offlineSilence() {
    var missedWindows = missedSyncWindowsForOffline == null || missedSyncWindowsForOffline < 1
        ? DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE
        : missedSyncWindowsForOffline;
    return SYNC_WINDOW.multipliedBy(missedWindows);
  }

  /**
   * Tells whether a unit whose last reading arrived at {@code lastReceivedAt} has been silent long
   * enough, at {@code now}, to count as offline. Reaching {@link #offlineSilence()} exactly already
   * counts.
   *
   * @param lastReceivedAt when the platform received the unit's latest reading; required
   * @param now            the instant to judge at; required
   * @return {@code true} when the unit counts as offline
   */
  public boolean isOfflineSince(Instant lastReceivedAt, Instant now) {
    Objects.requireNonNull(lastReceivedAt, "lastReceivedAt must not be null");
    Objects.requireNonNull(now, "now must not be null");
    return !Duration.between(lastReceivedAt, now).minus(offlineSilence()).isNegative();
  }
}
