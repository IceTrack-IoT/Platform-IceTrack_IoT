package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.servicerequests.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;

import java.util.List;
import java.util.Objects;

/**
 * Realizes {@link ReportGenerationStrategy} for
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType#TECHNICIAN_PERFORMANCE}.
 *
 * <p>Scoped by {@code filters.technicianId()}, an addition to the diagrammed
 * {@code ReportFilters} (see that class's Javadoc) without which this strategy would have no way
 * to know which technician to measure. Reports how many service requests the technician
 * completed within the window and their average rating across their whole history.</p>
 *
 * <p><strong>Pending dependency.</strong> Both underlying calls on
 * {@code ExternalServiceRequestsServiceForReporting} throw
 * {@link UnsupportedOperationException} until the Service Request and Feedback Management
 * context is built (see that port's Javadoc). This strategy is otherwise fully wired and will
 * work unmodified once that dependency exists.</p>
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
    double averageRating = serviceRequestsService.fetchAverageRatingByTechnician(technicianId);

    return List.of(
        new KpiValue("Completed service requests", (double) completedCount, "count"),
        new KpiValue("Average rating", averageRating, "stars"));
  }
}
