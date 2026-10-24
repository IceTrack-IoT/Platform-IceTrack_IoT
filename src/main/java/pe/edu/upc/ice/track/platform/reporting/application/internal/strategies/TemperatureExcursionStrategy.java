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
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType#TEMPERATURE_EXCURSION}.
 *
 * <p>For each unit in scope, reports how many {@code TEMPERATURE_EXCURSION} alerts opened within
 * the window and the cumulative hours spent outside the safe range. As with
 * {@link EquipmentUptimeStrategy}, scope is a single unit when {@code filters.equipmentId()} is
 * set, otherwise every unit at {@code filters.siteId()}.</p>
 */
@Service("TEMPERATURE_EXCURSION")
public class TemperatureExcursionStrategy implements ReportGenerationStrategy {

  private static final String TEMPERATURE_EXCURSION = "TEMPERATURE_EXCURSION";

  private final ExternalAssetsServiceForReporting assetsService;
  private final ExternalMonitoringServiceForReporting monitoringService;

  public TemperatureExcursionStrategy(
      ExternalAssetsServiceForReporting assetsService,
      ExternalMonitoringServiceForReporting monitoringService) {
    this.assetsService = assetsService;
    this.monitoringService = monitoringService;
  }

  @Override
  public List<KpiValue> generate(ReportFilters filters) {
    Objects.requireNonNull(filters, "filters must not be null");
    DateRange range = filters.dateRange();

    List<Long> equipmentIds = filters.equipmentId() != null
        ? List.of(filters.equipmentId().equipmentId())
        : assetsService.fetchEquipmentIdsBySite(filters.siteId().siteId());

    List<KpiValue> kpis = new ArrayList<>();
    long totalExcursions = 0;
    double totalHours = 0d;
    for (Long equipmentId : equipmentIds) {
      long excursions = monitoringService.countAlertsByEquipmentAndTypeInRange(
          equipmentId, TEMPERATURE_EXCURSION, range.from(), range.to());
      double hours = monitoringService.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
          equipmentId, TEMPERATURE_EXCURSION, range.from(), range.to());
      totalExcursions += excursions;
      totalHours += hours;
      kpis.add(new KpiValue("Excursions - Unit " + equipmentId, (double) excursions, "count"));
      kpis.add(new KpiValue("Excursion hours - Unit " + equipmentId, hours, "h"));
    }

    kpis.add(0, new KpiValue("Total excursion hours", totalHours, "h"));
    kpis.add(0, new KpiValue("Total excursions", (double) totalExcursions, "count"));
    return kpis;
  }
}
