package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Realizes {@link ReportGenerationStrategy} for
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType#TECHNICIAN_PERFORMANCE}.
 *
 * <p>Scoped by {@code filters.technicianId()}, an addition to the diagrammed
 * {@code ReportFilters} (see that class's Javadoc) without which this strategy would have no way
 * to know which technician to measure. Reports how many service requests the technician
 * completed within the window and their average rating across their whole history.</p>
 *
 * <p>A technician who has never been reviewed has no rating rather than a rating of zero, so the
 * "Average rating" KPI is left out of the report in that case instead of being reported as
 * {@code 0}.</p>
 */
@Service("TECHNICIAN_PERFORMANCE")
public class TechnicianPerformanceStrategy implements ReportGenerationStrategy {

  private final ExternalServiceRequestsServiceForReporting serviceRequestsService;

  public TechnicianPerformanceStrategy(ExternalServiceRequestsServiceForReporting serviceRequestsService) {
    this.serviceRequestsService = serviceRequestsService;
  }

  @Override
  public List<KpiValue> generate(ReportFilters filters) {
    Objects.requireNonNull(filters, "filters must not be null");
    Objects.requireNonNull(filters.technicianId(), "technicianId must not be null for a performance report");
    DateRange range = filters.dateRange();
    Long technicianId = filters.technicianId().technicianId();

    long completedCount = serviceRequestsService.countCompletedServiceRequestsByTechnicianInRange(
        technicianId, range.from(), range.to());
    Optional<Double> averageRating = serviceRequestsService.fetchAverageRatingByTechnician(technicianId);

    List<KpiValue> kpis = new ArrayList<>();
    kpis.add(new KpiValue("Completed service requests", (double) completedCount, "count"));
    averageRating.ifPresent(rating -> kpis.add(new KpiValue("Average rating", rating, "stars")));
    return kpis;
  }
}
