package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportResource;

/**
 * Converts a {@link Report} aggregate into a {@link ReportResource}.
 */
public class ReportResourceFromEntityAssembler {

  private ReportResourceFromEntityAssembler() {
  }

  /**
   * @param report the aggregate to convert
   * @return the equivalent resource
   */
  public static ReportResource toResourceFromEntity(Report report) {
    ReportFilters filters = report.getFilters();
    return new ReportResource(
        report.getId(),
        report.getTitle(),
        report.getType().name(),
        report.getFormat().name(),
        report.getStatus().name(),
        report.getResultPayload(),
        report.getUserId().userId(),
        filters.siteId() != null ? filters.siteId().siteId() : null,
        filters.equipmentId() != null ? filters.equipmentId().equipmentId() : null,
        filters.technicianId().technicianId(),
        filters.dateRange().from(),
        filters.dateRange().to(),
        report.getRequestedAt(),
        report.getCompletedAt());
  }
}
