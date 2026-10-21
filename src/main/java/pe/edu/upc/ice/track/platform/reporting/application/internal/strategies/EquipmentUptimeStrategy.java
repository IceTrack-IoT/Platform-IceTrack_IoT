package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.assets.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.monitoring.ExternalMonitoringServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Realizes {@link ReportGenerationStrategy} for
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType#EQUIPMENT_UPTIME}.
 *
 * <p>Uptime is derived from {@code DEVICE_OFFLINE} alert duration rather than from a dedicated
 * uptime record, since the real {@code monitoring} context has none: for each unit in scope, the
 * hours covered by offline alerts within the window are subtracted from the window's total
 * hours.</p>
 *
 * <p>Scope: when {@code filters.equipmentId()} is set, only that unit is measured; otherwise
 * every unit at {@code filters.siteId()} is measured and an uptime average is reported alongside
 * the per-unit figures. At least one of the two must be present, enforced at the point of use by
 * {@code ReportCommandServiceImpl} validating the command before invoking the strategy.</p>
 */
@Service("EQUIPMENT_UPTIME")
public class EquipmentUptimeStrategy implements ReportGenerationStrategy {

  private static final String DEVICE_OFFLINE = "DEVICE_OFFLINE";

  private final ExternalAssetsServiceForReporting assetsService;
  private final ExternalMonitoringServiceForReporting monitoringService;

  public EquipmentUptimeStrategy(
      ExternalAssetsServiceForReporting assetsService,
      ExternalMonitoringServiceForReporting monitoringService) {
    this.assetsService = assetsService;
    this.monitoringService = monitoringService;
  }

  @Override
  public List<KpiValue> generate(ReportFilters filters) {
    Objects.requireNonNull(filters, "filters must not be null");
    DateRange range = filters.dateRange();
    double windowHours = range.totalHours();

    List<Long> equipmentIds = filters.equipmentId() != null
        ? List.of(filters.equipmentId().equipmentId())
        : assetsService.fetchEquipmentIdsBySite(filters.siteId().siteId());

    List<KpiValue> kpis = new ArrayList<>();
    double uptimeSum = 0d;
    for (Long equipmentId : equipmentIds) {
      double offlineHours = monitoringService.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
          equipmentId, DEVICE_OFFLINE, range.from(), range.to());
      double uptimeHours = Math.max(0d, windowHours - offlineHours);
      double uptimePercentage = windowHours > 0 ? (uptimeHours / windowHours) * 100d : 0d;
      uptimeSum += uptimePercentage;
      kpis.add(new KpiValue("Uptime - Unit " + equipmentId, uptimePercentage, "%"));
    }

    if (!equipmentIds.isEmpty()) {
      kpis.add(0, new KpiValue("Average uptime", uptimeSum / equipmentIds.size(), "%"));
    } else {
      kpis.add(new KpiValue("Average uptime", 0d, "%"));
    }
    return kpis;
  }
}
