package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalMonitoringServiceForReporting;
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
 * <p>For each unit in scope it reports:</p>
 * <ul>
 *   <li>the excursion alerts opened within the window, and the hours spent under them;</li>
 *   <li>from its telemetry, the Mean Kinetic Temperature, the standard deviation of the temperatures,
 *       the degree-minutes outside the unit's threshold band, and the share of time inside the band.</li>
 * </ul>
 *
 * <p>Dismissed alerts never count, and neither does any reading or alert that falls inside a maintenance
 * window; both exclusions are applied by the monitoring context. When no unit in scope recorded any
 * telemetry in the window, the report fails with {@link InsufficientTelemetryException}.</p>
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
    int unitsWithTelemetry = 0;
    for (Long equipmentId : equipmentIds) {
      long excursions = monitoringService.countAlertsByEquipmentAndTypeInRange(
          equipmentId, TEMPERATURE_EXCURSION, range.from(), range.to());
      double hours = monitoringService.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
          equipmentId, TEMPERATURE_EXCURSION, range.from(), range.to());
      totalExcursions += excursions;
      totalHours += hours;
      kpis.add(new KpiValue("Excursions - Unit " + equipmentId, (double) excursions, "count"));
      kpis.add(new KpiValue("Excursion hours - Unit " + equipmentId, hours, "h"));

      var samples = monitoringService.fetchTemperatureSamplesByEquipmentInRange(equipmentId, range.from(), range.to());
      var band = assetsService.fetchTemperatureThresholdRange(equipmentId);
      if (samples.isEmpty() || band.isEmpty()) {
        continue;
      }
      unitsWithTelemetry++;
      var exposure = ThermalExposureCalculator.of(samples, band.get()[0], band.get()[1]);
      kpis.add(new KpiValue("Mean kinetic temperature - Unit " + equipmentId, exposure.meanKineticTemperature(), "C"));
      kpis.add(new KpiValue("Temperature standard deviation - Unit " + equipmentId, exposure.standardDeviation(), "C"));
      kpis.add(new KpiValue("Degree-minutes outside range - Unit " + equipmentId, exposure.degreeMinutes(), "degree-min"));
      kpis.add(new KpiValue("Time in range - Unit " + equipmentId, exposure.percentInRange(), "%"));
    }

    if (unitsWithTelemetry == 0) {
      throw new InsufficientTelemetryException();
    }

    kpis.add(0, new KpiValue("Total excursion hours", totalHours, "h"));
    kpis.add(0, new KpiValue("Total excursions", (double) totalExcursions, "count"));
    return kpis;
  }
}
