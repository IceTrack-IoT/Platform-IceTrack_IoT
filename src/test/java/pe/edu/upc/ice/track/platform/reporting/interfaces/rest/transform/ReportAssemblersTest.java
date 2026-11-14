package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.CreateReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportFiltersResource;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

class ReportAssemblersTest {

  @Nested
  class FromEntity {

    @Test
    @DisplayName("toResourceFromEntity(COMPLETED, url, kpis) -> full resource with payload, url and mapped KPI units (AAA)")
    void completed() {
      // Arrange
      var report = completedReport();
      var kpis = List.of(
          new KpiValue("a", "Count", 3.0, "count"),
          new KpiValue("b", "Mkt", 5.0, "C"),
          new KpiValue("c", "Dm", 7.0, "degree-min"),
          new KpiValue("d", "Pct", 9.0, "%"));

      // Act
      var resource = ReportResourceFromEntityAssembler.toResourceFromEntity(report, "https://dl", kpis);

      // Assert
      assertThat(resource.id()).isEqualTo(REPORT_ID);
      assertThat(resource.name()).isEqualTo(TITLE);
      assertThat(resource.type()).isEqualTo("EQUIPMENT_UPTIME");
      assertThat(resource.format()).isEqualTo("PDF");
      assertThat(resource.status()).isEqualTo("COMPLETED");
      assertThat(resource.failureReason()).isNull();
      assertThat(resource.resultPayload()).isEqualTo(report.getResultPayload());
      assertThat(resource.userId()).isEqualTo(USER_ID);
      assertThat(resource.siteId()).isEqualTo(SITE_ID);
      assertThat(resource.equipmentId()).isNull();
      assertThat(resource.technicianId()).isNull();
      assertThat(resource.serviceRequestId()).isNull();
      assertThat(resource.from()).isEqualTo(FROM);
      assertThat(resource.to()).isEqualTo(TO);
      assertThat(resource.requestedAt()).isEqualTo(report.getRequestedAt());
      assertThat(resource.completedAt()).isEqualTo(report.getCompletedAt());
      assertThat(resource.downloadUrl()).isEqualTo("https://dl");
      assertThat(resource.kpis()).extracting("unit").containsExactly("", "°C", "°C·min", "%");
      assertThat(resource.kpis()).extracting("key").containsExactly("a", "b", "c", "d");
      assertThat(resource.kpis().get(0).value()).isEqualTo(3.0);
      assertThat(resource.kpis().get(0).label()).isEqualTo("Count");
    }

    @Test
    @DisplayName("toResourceFromEntity(COMPLETED, null url, null kpis) -> optional parts stay null (AAA)")
    void completedWithoutExtras() {
      // Act
      var resource = ReportResourceFromEntityAssembler.toResourceFromEntity(completedReport(), null, null);

      // Assert
      assertThat(resource.downloadUrl()).isNull();
      assertThat(resource.kpis()).isNull();
    }

    @Test
    @DisplayName("toResourceFromEntity(FAILED) -> failureReason exposed, payload / KPIs / url hidden (AAA)")
    void failed() {
      // Act
      var resource = ReportResourceFromEntityAssembler.toResourceFromEntity(failedReport(), "https://dl", sampleKpis());

      // Assert
      assertThat(resource.status()).isEqualTo("FAILED");
      assertThat(resource.failureReason()).isEqualTo("Report generation failed");
      assertThat(resource.resultPayload()).isNull();
      assertThat(resource.kpis()).isNull();
      assertThat(resource.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("toResourceFromEntity(PENDING) -> no failure reason, payload or KPIs, no completion time (AAA)")
    void pending() {
      // Act
      var resource = ReportResourceFromEntityAssembler.toResourceFromEntity(pendingReport(), null, sampleKpis());

      // Assert
      assertThat(resource.status()).isEqualTo("PENDING");
      assertThat(resource.failureReason()).isNull();
      assertThat(resource.resultPayload()).isNull();
      assertThat(resource.kpis()).isNull();
      assertThat(resource.completedAt()).isNull();
    }

    @Test
    @DisplayName("toSummaryResourceFromEntity(COMPLETED) -> no payload, KPIs or download URL (AAA)")
    void summary() {
      // Act
      var resource = ReportResourceFromEntityAssembler.toSummaryResourceFromEntity(completedReport());

      // Assert
      assertThat(resource.status()).isEqualTo("COMPLETED");
      assertThat(resource.resultPayload()).isNull();
      assertThat(resource.kpis()).isNull();
      assertThat(resource.downloadUrl()).isNull();
      assertThat(resource.id()).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("toSummaryResourceFromEntity(FAILED) -> failure reason still shown (AAA)")
    void summaryFailed() {
      // Act & Assert
      assertThat(ReportResourceFromEntityAssembler.toSummaryResourceFromEntity(failedReport()).failureReason())
          .isEqualTo("Report generation failed");
    }

    @Test
    @DisplayName("toResourceFromEntity(report with every filter) -> equipment, technician and service request ids exposed (AAA)")
    void allFilters() {
      // Arrange
      var filters = new pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters(
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId(SITE_ID),
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId(EQUIPMENT_ID),
          new pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.TechnicianId(TECHNICIAN_ID), dateRange(), REQUEST_ID);
      var report = new Report(TITLE, ReportType.SERVICE_INTERVENTION, ReportFormat.CSV, new UserId(USER_ID), filters);

      // Act
      var resource = ReportResourceFromEntityAssembler.toSummaryResourceFromEntity(report);

      // Assert
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(resource.type()).isEqualTo("SERVICE_INTERVENTION");
      assertThat(resource.format()).isEqualTo("CSV");
      assertThat(resource.id()).isNull();
    }

    @Test
    @DisplayName("toPagedResourceFromPage(page) -> summaries plus paging metadata (AAA)")
    void paged() {
      // Arrange
      var page = new ReportPage(List.of(completedReport(), failedReport()), 1, 2, 5);

      // Act
      var resource = ReportResourceFromEntityAssembler.toPagedResourceFromPage(page);

      // Assert
      assertThat(resource.content()).hasSize(2);
      assertThat(resource.content()).extracting("status").containsExactly("COMPLETED", "FAILED");
      assertThat(resource.content().get(0).kpis()).isNull();
      assertThat(resource.page()).isEqualTo(1);
      assertThat(resource.size()).isEqualTo(2);
      assertThat(resource.totalElements()).isEqualTo(5);
      assertThat(resource.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("toPagedResourceFromPage(empty page) -> empty content, one page (AAA)")
    void pagedEmpty() {
      // Act
      var resource = ReportResourceFromEntityAssembler.toPagedResourceFromPage(ReportPage.empty(0, 20));

      // Assert
      assertThat(resource.content()).isEmpty();
      assertThat(resource.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("toResourceFromDownloadLink(link) -> copies every component (AAA)")
    void downloadLink() {
      // Arrange
      var link = new ReportDownloadLink("https://dl", TO, "r.pdf", "application/pdf", 99L);

      // Act
      var resource = ReportResourceFromEntityAssembler.toResourceFromDownloadLink(link);

      // Assert
      assertThat(resource.downloadUrl()).isEqualTo("https://dl");
      assertThat(resource.expiresAt()).isEqualTo(TO);
      assertThat(resource.fileName()).isEqualTo("r.pdf");
      assertThat(resource.contentType()).isEqualTo("application/pdf");
      assertThat(resource.sizeBytes()).isEqualTo(99L);
    }
  }

  @Nested
  class ToCommand {

    private CreateReportResource resource(String type, String format, ReportFiltersResource filters) {
      return new CreateReportResource("Name", type, format, filters);
    }

    @Test
    @DisplayName("toCommandFromResource(resource, username) -> command with parsed enums and every filter (AAA)")
    void maps() {
      // Arrange
      var filters = new ReportFiltersResource(SITE_ID, EQUIPMENT_ID, TECHNICIAN_ID, REQUEST_ID, FROM, TO);

      // Act
      var command = RequestReportCommandFromResourceAssembler.toCommandFromResource(
          resource("temperature_excursion", "xlsx", filters), USERNAME);

      // Assert
      assertThat(command.title()).isEqualTo("Name");
      assertThat(command.type()).isEqualTo(ReportType.TEMPERATURE_EXCURSION);
      assertThat(command.format()).isEqualTo(ReportFormat.XLSX);
      assertThat(command.username()).isEqualTo(USERNAME);
      assertThat(command.siteId()).isEqualTo(SITE_ID);
      assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(command.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(command.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(command.from()).isEqualTo(FROM);
      assertThat(command.to()).isEqualTo(TO);
    }

    @ParameterizedTest
    @CsvSource({"PDF,PDF", "' csv ',CSV", "Xlsx,XLSX"})
    @DisplayName("toCommandFromResource(format) -> trimmed and case-insensitive (AAA)")
    void formatParsing(String raw, ReportFormat expected) {
      // Act
      var command = RequestReportCommandFromResourceAssembler.toCommandFromResource(
          resource("EQUIPMENT_UPTIME", raw, new ReportFiltersResource(null, null, null, null, null, null)), USERNAME);

      // Assert
      assertThat(command.format()).isEqualTo(expected);
    }

    @Test
    @DisplayName("toCommandFromResource(unknown or null enum names) -> null type / format left to the service to reject (AAA)")
    void invalidEnums() {
      // Arrange
      var filters = new ReportFiltersResource(null, null, null, null, null, null);

      // Act
      var unknown = RequestReportCommandFromResourceAssembler.toCommandFromResource(resource("NOPE", "DOC", filters), USERNAME);
      var nulls = RequestReportCommandFromResourceAssembler.toCommandFromResource(resource(null, null, filters), USERNAME);

      // Assert
      assertThat(unknown.type()).isNull();
      assertThat(unknown.format()).isNull();
      assertThat(nulls.type()).isNull();
      assertThat(nulls.format()).isNull();
      assertThat(unknown.from()).isNull();
    }
  }

  @Test
  @DisplayName("ReportStatusResource / ReportTypeResource toDomain() -> same-named domain constants (AAA)")
  void enumResources() {
    // Act & Assert
    for (var status : pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportStatusResource.values()) {
      assertThat(status.toDomain()).isEqualTo(ReportStatus.valueOf(status.name()));
    }
    for (var type : pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportTypeResource.values()) {
      assertThat(type.toDomain()).isEqualTo(ReportType.valueOf(type.name()));
    }
    assertThat(pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportStatusResource.values()).hasSize(ReportStatus.values().length);
    assertThat(pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportTypeResource.values()).hasSize(ReportType.values().length);
  }
}
