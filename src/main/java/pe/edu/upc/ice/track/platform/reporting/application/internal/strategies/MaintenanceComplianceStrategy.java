package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.assets.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.servicerequests.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Realizes {@link ReportGenerationStrategy} for
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType#MAINTENANCE_COMPLIANCE}.
 *
 * <p>Scoped by {@code filters.siteId()} -- not by owner -- because
 * {@code AssetContextFacade}/{@code EquipmentRepository.findBySiteId} resolves a site's units
 * without requiring ownership, which lets {@code fetchEquipmentIdsBySite} be a real, working
 * facade call instead of a stub. Compliance is the share of the site's units that have at least
 * one completed preventive-maintenance service request within the window.</p>
 *
 * <p><strong>Pending dependency.</strong> The underlying call,
 * {@code ExternalServiceRequestsServiceForReporting#fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange},
 * throws {@link UnsupportedOperationException} until the Service Request and Feedback
 * Management context is built (see that port's Javadoc). This strategy is otherwise fully
 * wired and will work unmodified once that dependency exists.</p>
 */
@Service("MAINTENANCE_COMPLIANCE")
public class MaintenanceComplianceStrategy implements ReportGenerationStrategy {

  private final ExternalAssetsServiceForReporting assetsService;
  private final ExternalServiceRequestsServiceForReporting serviceRequestsService;

  public MaintenanceComplianceStrategy(
      ExternalAssetsServiceForReporting assetsService,
      ExternalServiceRequestsServiceForReporting serviceRequestsService) {
    this.assetsService = assetsService;
    this.serviceRequestsService = serviceRequestsService;
  }

  @Override
  public List<KpiValue> generate(ReportFilters filters) {
    Objects.requireNonNull(filters, "filters must not be null");
    Objects.requireNonNull(filters.siteId(), "siteId must not be null for a compliance report");
    DateRange range = filters.dateRange();
    Long siteId = filters.siteId().siteId();

    List<Long> siteEquipmentIds = assetsService.fetchEquipmentIdsBySite(siteId);
    Set<Long> compliantIds = Set.copyOf(
        serviceRequestsService.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(
            siteId, range.from(), range.to()));

    long compliantCount = siteEquipmentIds.stream().filter(compliantIds::contains).count();
    double compliancePercentage = siteEquipmentIds.isEmpty()
        ? 0d
        : (compliantCount * 100d) / siteEquipmentIds.size();

    return List.of(
        new KpiValue("Units in scope", (double) siteEquipmentIds.size(), "count"),
        new KpiValue("Compliant units", (double) compliantCount, "count"),
        new KpiValue("Compliance rate", compliancePercentage, "%"));
  }
}
