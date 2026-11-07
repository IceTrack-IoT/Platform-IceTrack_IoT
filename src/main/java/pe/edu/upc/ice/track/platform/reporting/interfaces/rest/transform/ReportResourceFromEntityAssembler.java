package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import org.springframework.lang.Nullable;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportResource;

/**
 * Converts a {@link Report} aggregate into a {@link ReportResource}.
 */
public class ReportResourceFromEntityAssembler {

  private ReportResourceFromEntityAssembler() {
  }

  /**
   * @param report      the aggregate to convert
   * @param downloadUrl the signed download URL, or {@code null} when the report has no file to offer
   * @return the equivalent resource
   */
  public static ReportResource toResourceFromEntity(Report report, @Nullable String downloadUrl) {
    ReportFilters filters = report.getFilters();
    return new ReportResource(
        report.getId(),
        report.getTitle(),
        report.getType().name(),
        report.getFormat().name(),
        report.getStatus().name(),
        report.getStatus() == ReportStatus.FAILED ? report.getResultPayload() : null,
        report.getStatus() == ReportStatus.COMPLETED ? report.getResultPayload() : null,
        report.getUserId().userId(),
        filters.siteId() != null ? filters.siteId().siteId() : null,
        filters.equipmentId() != null ? filters.equipmentId().equipmentId() : null,
        filters.technicianId() != null ? filters.technicianId().technicianId() : null,
        filters.dateRange().from(),
        filters.dateRange().to(),
        report.getRequestedAt(),
        report.getCompletedAt(),
        downloadUrl);
  }
}
