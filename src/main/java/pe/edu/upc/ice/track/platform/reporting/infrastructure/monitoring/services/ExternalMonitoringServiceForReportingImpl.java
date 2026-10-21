package pe.edu.upc.ice.track.platform.reporting.infrastructure.monitoring.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.monitoring.ExternalMonitoringServiceForReporting;

import java.time.LocalDateTime;

/**
 * Adapter for {@link ExternalMonitoringServiceForReporting}, wrapping the real
 * {@code monitoring} context.
 *
 * <p><strong>Facade extension required.</strong> {@code MonitoringContextFacade} -- whose own
 * Javadoc already states it is "consumed synchronously by Reporting & Análisis" -- does not yet
 * expose alert counts or durations scoped by unit, type and date range. Per
 * "Solo genera ese BC," that facade is not modified here; the patch below is documented instead,
 * to be applied by hand to {@code monitoring/interfaces/acl/MonitoringContextFacade.java} and its
 * {@code MonitoringContextFacadeImpl}:</p>
 *
 * <pre>{@code
 * // MonitoringContextFacade (interface):
 * long countAlertsByEquipmentAndTypeInRange(Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to);
 * double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to);
 *
 * // MonitoringContextFacadeImpl (it already injects an AlertRepository):
 * public long countAlertsByEquipmentAndTypeInRange(Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
 *   var type = AlertType.valueOf(alertType);
 *   return alertRepository.findByEquipmentIdAndTypeAndOpenedAtBetween(equipmentId, type, from, to).size();
 * }
 *
 * public double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
 *   var type = AlertType.valueOf(alertType);
 *   return alertRepository.findByEquipmentIdAndTypeAndOpenedAtBetween(equipmentId, type, from, to).stream()
 *       .mapToDouble(alert -> {
 *         var start = alert.getOpenedAt().isBefore(from) ? from : alert.getOpenedAt();
 *         var end = alert.getResolvedAt() == null || alert.getResolvedAt().isAfter(to) ? to : alert.getResolvedAt();
 *         return Duration.between(start, end).toMinutes() / 60.0;
 *       })
 *       .sum();
 * }
 * }</pre>
 *
 * <p>The duration computation deliberately uses {@code openedAt}/{@code resolvedAt} rather than
 * {@code AlertPersistenceEntity.excursionDurationSeconds}, because that field is {@code null} for
 * {@code DEVICE_OFFLINE} alerts (per the entity's own Javadoc) and this port is used for both
 * alert types. Until the patch above is applied, this adapter throws
 * {@link UnsupportedOperationException}.</p>
 */
@Service
public class ExternalMonitoringServiceForReportingImpl implements ExternalMonitoringServiceForReporting {

  // private final MonitoringContextFacade monitoringContextFacade;
  //
  // public ExternalMonitoringServiceForReportingImpl(MonitoringContextFacade monitoringContextFacade) {
  //   this.monitoringContextFacade = monitoringContextFacade;
  // }

  @Override
  public long countAlertsByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
    // return monitoringContextFacade.countAlertsByEquipmentAndTypeInRange(equipmentId, alertType, from, to);
    throw new UnsupportedOperationException(
        "MonitoringContextFacade.countAlertsByEquipmentAndTypeInRange(...) does not exist yet -- "
            + "see this class's Javadoc for the exact patch to apply.");
  }

  @Override
  public double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
    // return monitoringContextFacade.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(equipmentId, alertType, from, to);
    throw new UnsupportedOperationException(
        "MonitoringContextFacade.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(...) does not exist yet -- "
            + "see this class's Javadoc for the exact patch to apply.");
  }
}
