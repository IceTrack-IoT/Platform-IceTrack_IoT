package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound service (of the Anti-Corruption Layer between {@code reporting} and {@code monitoring})
 * through which {@code EquipmentUptimeStrategy} and {@code TemperatureExcursionStrategy} read
 * alert history.
 *
 * <p>This is the only class in {@code reporting} that knows a {@code monitoring} context exists.
 * It talks to it exclusively through {@link MonitoringContextFacade}: no Monitoring aggregate,
 * repository or command is reachable from here, and {@code alertType} is passed as the plain enum
 * name ({@code "DEVICE_OFFLINE"} or {@code "TEMPERATURE_EXCURSION"}) rather than as the
 * monitoring-owned {@code AlertType} type.</p>
 */
@Service
public class ExternalMonitoringServiceForReporting {

  private final MonitoringContextFacade monitoringContextFacade;

  public ExternalMonitoringServiceForReporting(MonitoringContextFacade monitoringContextFacade) {
    this.monitoringContextFacade = monitoringContextFacade;
  }

  /**
   * @param equipmentId identifier of the unit; required
   * @param alertType   the alert type to count, by its enum name; required
   * @param from        inclusive start of the window
   * @param to          inclusive end of the window
   * @return the number of alerts of that type opened for the unit within the window
   */
  public long countAlertsByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
    return monitoringContextFacade.countAlertsByEquipmentAndTypeInRange(equipmentId, alertType, from, to);
  }

  /**
   * @param equipmentId identifier of the unit; required
   * @param alertType   the alert type to measure, by its enum name; required
   * @param from        inclusive start of the window
   * @param to          inclusive end of the window
   * @return total hours the condition applied within the window, clamped to it -- an alert that
   *         started before {@code from} or is still open past {@code to} only contributes the
   *         portion that overlaps the window
   */
  public double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
    return monitoringContextFacade.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
        equipmentId, alertType, from, to);
  }

  /**
   * @param equipmentId identifier of the unit; required
   * @param from        inclusive start of the window
   * @param to          inclusive end of the window
   * @return the unit's temperature samples within the window, outside maintenance, as
   *         {@code {celsius, epochSeconds}} rows in time order; empty when there are none
   */
  public List<double[]> fetchTemperatureSamplesByEquipmentInRange(Long equipmentId, LocalDateTime from, LocalDateTime to) {
    return monitoringContextFacade.fetchTemperatureSamplesByEquipmentInRange(equipmentId, from, to);
  }
}
