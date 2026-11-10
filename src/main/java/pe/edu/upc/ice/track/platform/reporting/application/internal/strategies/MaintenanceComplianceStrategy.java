package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
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
 * {@code AssetContextFacade.fetchEquipmentIdsBySite} resolves a site's units without requiring
 * ownership. Compliance is the share of the site's units that have at least one completed
 * preventive-maintenance service request within the window; the completions come from
 * {@code ServiceRequestContextFacade} and are intersected with the site's current units.</p>
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
        new KpiValue("units_in_scope", "Units in scope", (double) siteEquipmentIds.size(), "count"),
        new KpiValue("compliant_units", "Compliant units", (double) compliantCount, "count"),
        new KpiValue("compliance_rate_pct", "Compliance rate", compliancePercentage, "%"));
  }
}
