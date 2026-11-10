package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ACL facade that exposes Monitoring and Alerting capabilities to other bounded contexts.
 *
 * <p>Every parameter and return type is a primitive, a boxed primitive, a {@link String}, or an
 * array, collection or map of those; no monitoring aggregate, value object or repository is
 * reachable from here.</p>
 *
 * <p>The range queries used by Reporting exclude what is not part of the unit's operating time:
 * alerts judged false alarms ({@code DISMISSED}) are ignored, and so is every interval a maintenance
 * window covered. Alerts opened inside a window are not counted, alert durations lose the part that
 * overlaps a window, and telemetry recorded during maintenance is not returned.</p>
 */
@Service
public class MonitoringContextFacade {

  private static final long NO_ALERT = 0L;

  private static final List<AlertStatus> ACTIVE_STATUSES = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

  /**
   * How far before a range a maintenance window may have started and still overlap the range. Windows
   * are closed by the expiry sweep soon after their expiration, so a week is a generous bound.
   */
  private static final Duration MAINTENANCE_LOOKBACK = Duration.ofDays(7);

  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final MaintenanceWindowRepository maintenanceWindowRepository;
  private final SensorReadingRepository sensorReadingRepository;

  public MonitoringContextFacade(
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      MaintenanceWindowRepository maintenanceWindowRepository,
      SensorReadingRepository sensorReadingRepository) {
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.maintenanceWindowRepository = maintenanceWindowRepository;
    this.sensorReadingRepository = sensorReadingRepository;
  }

  /**
   * Returns the alert policy that applies to a unit: its own when it has one, the stored platform
   * default otherwise, and the built-in default when neither is stored.
   *
   * <p>Device Management includes it in the configuration the unit's edge gateway polls, which
   * evaluates excursions locally.</p>
   *
   * @param equipmentId identifier of the unit; {@code null} yields the platform default
   * @return the applicable settings, never {@code null}
   */
  @Transactional(readOnly = true)
  public AlertPolicySettings fetchApplicableAlertPolicy(Long equipmentId) {
    var policy = (equipmentId == null
        ? alertPolicyRepository.findDefaultPolicy()
        : alertPolicyRepository.findApplicableTo(equipmentId))
        .orElseGet(AlertPolicy::platformDefault);
    return new AlertPolicySettings(
        policy.getSustainedExcursionMinutes(),
        policy.getHysteresisMargin().celsius(),
        policy.getMissedSyncWindowsForOffline());
  }

  /**
   * Returns how long each unit may stay silent before it counts as offline, the window the scheduled
   * offline detection applies: one minute times the {@code missed_sync_windows_for_offline} of the
   * policy applicable to the unit (its own, the stored default, or the built-in default).
   *
   * <p>Assets Management calls this to evaluate the connectivity of a whole page of units at once:
   * the units' own policies are read in one statement, and the default once.</p>
   *
   * @param equipmentIds identifiers of the units; {@code null} entries are ignored
   * @return the window in seconds, for every unit asked about
   */
  @Transactional(readOnly = true)
  public Map<Long, Long> fetchOfflineSilenceSeconds(Collection<Long> equipmentIds) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return Map.of();
    }
    var ids = equipmentIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (ids.isEmpty()) {
      return Map.of();
    }
    var ownPolicies = alertPolicyRepository.findByEquipmentIdIn(ids).stream()
        .filter(policy -> policy.getEquipmentId() != null)
        .collect(Collectors.toMap(policy -> policy.getEquipmentId().equipmentId(), policy -> policy, (first, second) -> first));
    var fallback = ownPolicies.size() == ids.size()
        ? null
        : alertPolicyRepository.findDefaultPolicy().orElseGet(AlertPolicy::platformDefault);
    var windows = new HashMap<Long, Long>();
    for (var equipmentId : ids) {
      var policy = ownPolicies.getOrDefault(equipmentId, fallback);
      windows.put(equipmentId, policy.offlineSilence().toSeconds());
    }
    return Map.copyOf(windows);
  }

  /**
   * Returns the unit an alert was raised on, whatever its status and owner.
   *
   * <p>Service Requests calls this to link a corrective request to the alert that prompted it; it
   * checks itself that the unit belongs to the caller.</p>
   *
   * @param alertId identifier of the alert
   * @return the unit identifier, or {@code 0L} when the identifier is {@code null} or no alert carries it
   */
  @Transactional(readOnly = true)
  public Long fetchAlertEquipmentId(Long alertId) {
    if (alertId == null) {
      return NO_ALERT;
    }
    return alertRepository.findById(alertId).map(alert -> alert.getEquipmentId()).orElse(NO_ALERT);
  }

  /** @return the number of OPEN or ACKNOWLEDGED alerts currently active for the equipment. */
  public long countOpenAlertsByEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return 0L;
    }
    return alertRepository.findByEquipmentIdAndStatusIn(equipmentId, ACTIVE_STATUSES).size();
  }

  /** @return the identifier of the most recent alert matching the status, or {@code 0L}. */
  public Long fetchLatestAlertIdByEquipmentAndStatus(Long equipmentId, String status) {
    if (equipmentId == null || status == null) {
      return NO_ALERT;
    }
    AlertStatus parsedStatus;
    try {
      parsedStatus = AlertStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return NO_ALERT;
    }
    return alertRepository.findByEquipmentIdAndStatusIn(equipmentId, ACTIVE_STATUSES).stream()
        .filter(alert -> alert.getStatus() == parsedStatus)
        .findFirst()
        .map(alert -> alert.getId() == null ? NO_ALERT : alert.getId())
        .orElse(NO_ALERT);
  }

  /**
   * Counts the alerts of one type that were opened within a range.
   *
   * <p>Dismissed alerts are not counted, and neither are alerts opened while a maintenance window of
   * the unit was open.</p>
   *
   * @param equipmentId identifier of the unit
   * @param alertType   the alert type, by its enum name
   * @param from        inclusive start of the range
   * @param to          inclusive end of the range
   * @return how many alerts of the type were opened for the equipment within the range, outside maintenance
   */
  public long countAlertsByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, Instant from, Instant to) {
    var type = AlertType.valueOf(alertType.toUpperCase());
    var windows = maintenanceIntervals(equipmentId, from, to);
    return alertRepository.findByEquipmentIdAndTypeOpenedBetween(equipmentId, type, from, to).stream()
        .filter(alert -> alert.getStatus() != AlertStatus.DISMISSED)
        .filter(alert -> windows.stream().noneMatch(window -> window.contains(alert.getOpenedAt())))
        .count();
  }

  /**
   * Totals the hours a unit spent under alerts of one type within a range.
   *
   * <p>Each alert contributes the part of its duration that falls inside the range, minus the part
   * that falls inside a maintenance window. Dismissed alerts contribute nothing.</p>
   *
   * @param equipmentId identifier of the unit
   * @param alertType   the alert type, by its enum name
   * @param from        inclusive start of the range
   * @param to          inclusive end of the range
   * @return total hours under alerts of the type within the range, excluding maintenance
   */
  public double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, Instant from, Instant to) {
    var type = AlertType.valueOf(alertType.toUpperCase());
    var windows = maintenanceIntervals(equipmentId, from, to);
    return alertRepository.findByEquipmentIdAndTypeOpenedBetween(equipmentId, type, from, to).stream()
        .filter(alert -> alert.getStatus() != AlertStatus.DISMISSED)
        .mapToDouble(alert -> {
          var start = alert.getOpenedAt().isBefore(from) ? from : alert.getOpenedAt();
          var end = alert.getResolvedAt() == null || alert.getResolvedAt().isAfter(to) ? to : alert.getResolvedAt();
          if (!end.isAfter(start)) {
            return 0d;
          }
          var seconds = Duration.between(start, end).toSeconds();
          for (var window : windows) {
            seconds -= window.overlapSeconds(start, end);
          }
          return Math.max(0, seconds) / 3600.0;
        })
        .sum();
  }

  /**
   * Returns the temperature samples a unit recorded within a range, outside maintenance.
   *
   * <p>Each row is {@code {celsius, epochSeconds}}: the calibrated average temperature of one reading
   * and the moment it was recorded, in seconds since the epoch. Rows come in time order.</p>
   *
   * @param equipmentId identifier of the unit
   * @param from        inclusive start of the range
   * @param to          inclusive end of the range
   * @return the samples, empty when the unit recorded none in the range
   */
  public List<double[]> fetchTemperatureSamplesByEquipmentInRange(
      Long equipmentId, Instant from, Instant to) {
    if (equipmentId == null) {
      return List.of();
    }
    return sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(equipmentId, from, to).stream()
        .filter(reading -> !reading.isUnderMaintenance())
        .filter(reading -> reading.getAvgTemperature() != null && reading.getAvgTemperature().celsius() != null)
        .sorted(Comparator.comparing(SensorReading::getRecordedAt))
        .map(reading -> new double[] {
            reading.getAvgTemperature().celsius(),
            reading.getRecordedAt().getEpochSecond()})
        .toList();
  }

  /**
   * Loads the maintenance windows of a unit that overlap a range, clipped to it and merged.
   *
   * @param equipmentId identifier of the unit; {@code null} yields no intervals
   * @param from        inclusive start of the range
   * @param to          inclusive end of the range
   * @return the merged maintenance intervals, sorted by start
   */
  private List<Interval> maintenanceIntervals(Long equipmentId, Instant from, Instant to) {
    if (equipmentId == null) {
      return List.of();
    }
    var searchFrom = from.minus(MAINTENANCE_LOOKBACK);
    var searchTo = to;
    var raw = new ArrayList<Interval>();
    for (var window : maintenanceWindowRepository.findByEquipmentIdAndStartedAtBetween(equipmentId, searchFrom, searchTo)) {
      var start = window.getStartedAt();
      var end = window.getEndedAt() == null ? to : window.getEndedAt();
      var clipped = new Interval(start.isBefore(from) ? from : start, end.isAfter(to) ? to : end);
      if (clipped.end().isAfter(clipped.start())) {
        raw.add(clipped);
      }
    }
    return Interval.merge(raw);
  }

  /**
   * A half-open span of time, {@code [start, end)}.
   *
   * @param start inclusive start
   * @param end   exclusive end
   */
  private record Interval(Instant start, Instant end) {

    boolean contains(Instant instant) {
      return instant != null && !instant.isBefore(start) && instant.isBefore(end);
    }

    long overlapSeconds(Instant from, Instant to) {
      var overlapStart = start.isAfter(from) ? start : from;
      var overlapEnd = end.isBefore(to) ? end : to;
      return overlapEnd.isAfter(overlapStart) ? Duration.between(overlapStart, overlapEnd).toSeconds() : 0L;
    }

    static List<Interval> merge(List<Interval> intervals) {
      var sorted = intervals.stream().sorted(Comparator.comparing(Interval::start)).toList();
      var merged = new ArrayList<Interval>();
      for (var interval : sorted) {
        var last = merged.isEmpty() ? null : merged.getLast();
        if (last != null && !interval.start().isAfter(last.end())) {
          merged.set(merged.size() - 1, new Interval(last.start(), interval.end().isAfter(last.end()) ? interval.end() : last.end()));
        } else {
          merged.add(interval);
        }
      }
      return merged;
    }
  }
}
