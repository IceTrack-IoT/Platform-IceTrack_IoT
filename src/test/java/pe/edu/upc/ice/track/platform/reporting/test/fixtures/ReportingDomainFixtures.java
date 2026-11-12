package pe.edu.upc.ice.track.platform.reporting.test.fixtures;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFile;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.TechnicianId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.List;

public final class ReportingDomainFixtures {

  public static final Long USER_ID = 9L;
  public static final Long OTHER_USER_ID = 10L;
  public static final Long OWNER_ID = 5L;
  public static final Long SITE_ID = 3L;
  public static final Long EQUIPMENT_ID = 10L;
  public static final Long TECHNICIAN_ID = 6L;
  public static final Long REQUEST_ID = 77L;
  public static final Long REPORT_ID = 500L;
  public static final String USERNAME = "atorres";
  public static final String TITLE = "Monthly report";
  public static final String FILE_KEY = "reports/5/500.pdf";
  public static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
  public static final Instant TO = Instant.parse("2026-01-02T00:00:00Z");

  private ReportingDomainFixtures() {
  }

  public static DateRange dateRange() {
    return new DateRange(FROM, TO);
  }

  public static ReportFilters siteFilters() {
    return new ReportFilters(new SiteId(SITE_ID), null, null, dateRange());
  }

  public static ReportFilters equipmentFilters() {
    return new ReportFilters(null, new EquipmentId(EQUIPMENT_ID), null, dateRange());
  }

  public static ReportFilters technicianFilters() {
    return new ReportFilters(null, null, new TechnicianId(TECHNICIAN_ID), dateRange());
  }

  public static ReportFilters requestFilters() {
    return new ReportFilters(null, null, null, dateRange(), REQUEST_ID);
  }

  public static ReportFile reportFile() {
    return new ReportFile(FILE_KEY, "application/pdf", 2048L);
  }

  public static Report report() {
    return new Report(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, new UserId(USER_ID), siteFilters());
  }

  public static Report pendingReport() {
    var report = report();
    report.assignId(REPORT_ID);
    return report;
  }

  public static Report generatingReport() {
    var report = pendingReport();
    report.markAsGenerating();
    return report;
  }

  public static Report completedReport() {
    var report = generatingReport();
    report.markAsCompleted("[{\"key\":\"average_uptime_pct\",\"label\":\"Average uptime\",\"value\":99.0,\"unit\":\"%\"}]",
        reportFile());
    return report;
  }

  public static Report failedReport() {
    var report = generatingReport();
    report.markAsFailed("Report generation failed");
    return report;
  }

  public static List<KpiValue> sampleKpis() {
    return List.of(new KpiValue("average_uptime_pct", "Average uptime", 99.0, "%"));
  }
}
