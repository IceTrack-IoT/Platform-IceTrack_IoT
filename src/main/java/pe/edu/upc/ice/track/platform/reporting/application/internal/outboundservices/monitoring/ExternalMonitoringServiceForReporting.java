package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.monitoring;

import java.time.LocalDateTime;

/**
 * Outbound port (of the Anti-Corruption Layer between {@code reporting} and {@code monitoring})
 * through which {@code EquipmentUptimeStrategy} and {@code TemperatureExcursionStrategy} read
 * alert history.
 *
 * <p>The reporting application layer depends on this port and on nothing else: it never sees a
 * Monitoring aggregate, repository or command, and {@code alertType} is passed as the plain enum
 * name ({@code "DEVICE_OFFLINE"} or {@code "TEMPERATURE_EXCURSION"}) rather than as the
 * monitoring-owned {@code AlertType} type. The adapter implementing it, in
 * {@code reporting.infrastructure.monitoring.services}, is the only class in {@code reporting}
 * allowed to know that a {@code monitoring} context exists.</p>
 */
public interface ExternalMonitoringServiceForReporting {

  /**
   * @param equipmentId identifier of the unit; required
   * @param alertType   the alert type to count, by its enum name; required
   * @param from        inclusive start of the window
   * @param to          inclusive end of the window
   * @return the number of alerts of that type opened for the unit within the window
   */
  long countAlertsByEquipmentAndTypeInRange(Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to);

  /**
   * @param equipmentId identifier of the unit; required
   * @param alertType   the alert type to measure, by its enum name; required
   * @param from        inclusive start of the window
   * @param to          inclusive end of the window
   * @return total hours the condition applied within the window, clamped to it -- an alert that
   *         started before {@code from} or is still open past {@code to} only contributes the
   *         portion that overlaps the window
   */
  double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to);
}
