package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import org.springframework.lang.Nullable;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.KpiResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.PagedReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportDownloadLinkResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportResource;

import java.util.List;
import java.util.Map;

/**
 * Converts a {@link Report} aggregate into a {@link ReportResource}, and its download link into a
 * {@link ReportDownloadLinkResource}.
 */
public class ReportResourceFromEntityAssembler {

  /**
   * How the units the strategies record are written in {@code kpis}: a plain count carries no unit,
   * and temperatures use the degree sign. Any other unit is passed through as recorded.
   */
  private static final Map<String, String> KPI_UNITS = Map.of(
      "count", "",
      "C", "°C",
      "degree-min", "°C·min");

  private ReportResourceFromEntityAssembler() {
  }

  /**
   * Converts a report in full: its payload, its KPIs and, when given, its download URL.
   *
   * @param report      the aggregate to convert
   * @param downloadUrl the signed download URL, or {@code null} when the report has no file to offer
   * @param kpis        the KPIs read from the payload, or {@code null} when the report is not
   *                    completed or its payload cannot be read
   * @return the equivalent resource
   */
  public static ReportResource toResourceFromEntity(
      Report report, @Nullable String downloadUrl, @Nullable List<KpiValue> kpis) {
    return toResource(report, downloadUrl, kpis, false);
  }

  /**
   * Converts a report into a summary, without its payload, its KPIs or a download URL.
   *
   * @param report the aggregate to convert
   * @return the summary resource
   */
  public static ReportResource toSummaryResourceFromEntity(Report report) {
    return toResource(report, null, null, true);
  }

  /**
   * Converts a page of reports into its paged resource, whose items are summaries.
   *
   * @param page the page to convert
   * @return the paged resource
   */
  public static PagedReportResource toPagedResourceFromPage(ReportPage page) {
    return new PagedReportResource(
        page.content().stream().map(ReportResourceFromEntityAssembler::toSummaryResourceFromEntity).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  /**
   * Converts a download link into its resource.
   *
   * @param link the link
   * @return the resource
   */
  public static ReportDownloadLinkResource toResourceFromDownloadLink(ReportDownloadLink link) {
    return new ReportDownloadLinkResource(
        link.url(), link.expiresAt(), link.fileName(), link.contentType(), link.sizeBytes());
  }

  private static ReportResource toResource(
      Report report, @Nullable String downloadUrl, @Nullable List<KpiValue> kpis, boolean summary) {
    ReportFilters filters = report.getFilters();
    var completed = report.getStatus() == ReportStatus.COMPLETED;
    return new ReportResource(
        report.getId(),
        report.getTitle(),
        report.getType().name(),
        report.getFormat().name(),
        report.getStatus().name(),
        report.getStatus() == ReportStatus.FAILED ? report.getResultPayload() : null,
        completed && !summary ? report.getResultPayload() : null,
        report.getUserId().userId(),
        filters.siteId() != null ? filters.siteId().siteId() : null,
        filters.equipmentId() != null ? filters.equipmentId().equipmentId() : null,
        filters.technicianId() != null ? filters.technicianId().technicianId() : null,
        filters.serviceRequestId(),
        filters.dateRange().from(),
        filters.dateRange().to(),
        report.getRequestedAt(),
        report.getCompletedAt(),
        summary ? null : downloadUrl,
        completed && !summary && kpis != null ? kpis.stream().map(ReportResourceFromEntityAssembler::toKpiResource).toList() : null);
  }

  private static KpiResource toKpiResource(KpiValue kpi) {
    return new KpiResource(kpi.key(), kpi.label(), kpi.value(), KPI_UNITS.getOrDefault(kpi.unit(), kpi.unit()));
  }
}
