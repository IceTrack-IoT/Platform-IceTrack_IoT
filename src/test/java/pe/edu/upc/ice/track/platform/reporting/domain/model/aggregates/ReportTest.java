package pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFile;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.FILE_KEY;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.REPORT_ID;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.TITLE;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.completedReport;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.failedReport;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.generatingReport;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.pendingReport;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.report;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.reportFile;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.siteFilters;

class ReportTest {

  private static Report titled(String title, ReportFormat format, ReportFile file) {
    var report = new Report(title, ReportType.EQUIPMENT_UPTIME, format, new UserId(USER_ID), siteFilters());
    report.assignId(REPORT_ID);
    report.markAsGenerating();
    report.markAsCompleted("[]", file);
    return report;
  }

  @Nested
  class Construction {

    @Test
    @DisplayName("new Report(...) -> PENDING with request time set and no id, payload or file (AAA)")
    void startsPending() {
      // Arrange
      var before = Instant.now();

      // Act
      var report = report();

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
      assertThat(report.getId()).isNull();
      assertThat(report.getResultPayload()).isNull();
      assertThat(report.getFile()).isNull();
      assertThat(report.getCompletedAt()).isNull();
      assertThat(report.getRequestedAt()).isBetween(before, Instant.now().plusSeconds(1));
      assertThat(report.getTitle()).isEqualTo(TITLE);
    }

    @Test
    @DisplayName("new Report(null part) -> NullPointerException naming the part (AAA)")
    void nullPartsRejected() {
      // Act & Assert
      assertThatThrownBy(() -> new Report(null, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, new UserId(USER_ID), siteFilters()))
          .hasMessage("title must not be null");
      assertThatThrownBy(() -> new Report(TITLE, null, ReportFormat.PDF, new UserId(USER_ID), siteFilters()))
          .hasMessage("type must not be null");
      assertThatThrownBy(() -> new Report(TITLE, ReportType.EQUIPMENT_UPTIME, null, new UserId(USER_ID), siteFilters()))
          .hasMessage("format must not be null");
      assertThatThrownBy(() -> new Report(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, null, siteFilters()))
          .hasMessage("userId must not be null");
      assertThatThrownBy(() -> new Report(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, new UserId(USER_ID), null))
          .hasMessage("filters must not be null");
    }

    @Test
    @DisplayName("Report.rehydrate(...) -> restores every stored field exactly (AAA)")
    void rehydrate() {
      // Arrange
      var requested = Instant.parse("2026-02-01T10:00:00Z");
      var completed = Instant.parse("2026-02-01T10:05:00Z");

      // Act
      var report = Report.rehydrate(REPORT_ID, TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.XLSX,
          ReportStatus.COMPLETED, "[]", new UserId(USER_ID), siteFilters(), requested, completed, reportFile());

      // Assert
      assertThat(report.getId()).isEqualTo(REPORT_ID);
      assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
      assertThat(report.getResultPayload()).isEqualTo("[]");
      assertThat(report.getRequestedAt()).isEqualTo(requested);
      assertThat(report.getCompletedAt()).isEqualTo(completed);
      assertThat(report.getFile()).isEqualTo(reportFile());
    }

    @Test
    @DisplayName("isOwnedBy(UserId) -> true only for the requesting user (AAA)")
    void ownership() {
      // Act & Assert
      assertThat(report().isOwnedBy(new UserId(USER_ID))).isTrue();
      assertThat(report().isOwnedBy(new UserId(USER_ID + 1))).isFalse();
      assertThat(report().isOwnedBy(null)).isFalse();
    }
  }

  @Nested
  class Lifecycle {

    @Test
    @DisplayName("markAsGenerating() on PENDING -> GENERATING (AAA)")
    void generating() {
      // Arrange
      var report = pendingReport();

      // Act
      report.markAsGenerating();

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
    }

    @Test
    @DisplayName("markAsGenerating() on a non-PENDING report -> IllegalStateException (AAA)")
    void generatingOnlyFromPending() {
      // Act & Assert
      assertThatThrownBy(() -> generatingReport().markAsGenerating())
          .isInstanceOf(IllegalStateException.class).hasMessage("Only a PENDING report can start generating, was GENERATING");
      assertThatThrownBy(() -> completedReport().markAsGenerating()).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> failedReport().markAsGenerating()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("markAsCompleted(payload, file) on GENERATING -> COMPLETED with payload, file and completion time (AAA)")
    void completed() {
      // Arrange
      var report = generatingReport();
      var before = Instant.now();

      // Act
      report.markAsCompleted("[1]", reportFile());

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
      assertThat(report.getResultPayload()).isEqualTo("[1]");
      assertThat(report.getFile()).isEqualTo(reportFile());
      assertThat(report.getCompletedAt()).isBetween(before, Instant.now().plusSeconds(1));
    }

    @Test
    @DisplayName("markAsCompleted(...) outside GENERATING or with nulls -> exception and state untouched (AAA)")
    void completedGuards() {
      // Arrange
      var pending = pendingReport();
      var generating = generatingReport();

      // Act & Assert
      assertThatThrownBy(() -> pending.markAsCompleted("[]", reportFile()))
          .isInstanceOf(IllegalStateException.class).hasMessage("Only a GENERATING report can be completed, was PENDING");
      assertThatThrownBy(() -> generating.markAsCompleted(null, reportFile())).hasMessage("payload must not be null");
      assertThatThrownBy(() -> generating.markAsCompleted("[]", null)).hasMessage("file must not be null");
      assertThat(generating.getStatus()).isEqualTo(ReportStatus.GENERATING);
    }

    @Test
    @DisplayName("markAsFailed(reason) on GENERATING -> FAILED keeping the reason as payload (AAA)")
    void failed() {
      // Arrange
      var report = generatingReport();

      // Act
      report.markAsFailed("boom");

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("boom");
      assertThat(report.getCompletedAt()).isNotNull();
      assertThat(report.getFile()).isNull();
    }

    @Test
    @DisplayName("markAsFailed(...) outside GENERATING or null reason -> exception (AAA)")
    void failedGuards() {
      // Act & Assert
      assertThatThrownBy(() -> pendingReport().markAsFailed("x"))
          .isInstanceOf(IllegalStateException.class).hasMessage("Only a GENERATING report can be failed, was PENDING");
      assertThatThrownBy(() -> completedReport().markAsFailed("x")).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> generatingReport().markAsFailed(null)).hasMessage("reason must not be null");
    }

    @Test
    @DisplayName("canBeDeleted() -> true only for COMPLETED and FAILED (AAA)")
    void deletable() {
      // Act & Assert
      assertThat(pendingReport().canBeDeleted()).isFalse();
      assertThat(generatingReport().canBeDeleted()).isFalse();
      assertThat(completedReport().canBeDeleted()).isTrue();
      assertThat(failedReport().canBeDeleted()).isTrue();
    }

    @Test
    @DisplayName("hasFile() -> true only for a COMPLETED report holding a file (AAA)")
    void hasFile() {
      // Arrange
      var completedWithoutFile = Report.rehydrate(REPORT_ID, TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF,
          ReportStatus.COMPLETED, "[]", new UserId(USER_ID), siteFilters(), Instant.now(), Instant.now(), null);
      var failedWithFile = Report.rehydrate(REPORT_ID, TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF,
          ReportStatus.FAILED, "x", new UserId(USER_ID), siteFilters(), Instant.now(), Instant.now(), reportFile());

      // Act & Assert
      assertThat(completedReport().hasFile()).isTrue();
      assertThat(completedWithoutFile.hasFile()).isFalse();
      assertThat(failedWithFile.hasFile()).isFalse();
      assertThat(pendingReport().hasFile()).isFalse();
    }
  }

  @Nested
  class SuggestedFileName {

    @Test
    @DisplayName("suggestedFileName() -> slug of the title plus the stored file extension (AAA)")
    void slugAndExtension() {
      // Act & Assert
      assertThat(completedReport().suggestedFileName()).isEqualTo("monthly-report.pdf");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Informe de Mantención Ñandú | informe-de-mantencion-nandu",
        "  --Q1 / 2026: Uptime!!--  | q1-2026-uptime",
        "UPPER lower 123             | upper-lower-123"})
    @DisplayName("suggestedFileName() -> accents stripped, lower-cased, non-alphanumerics collapsed to single dashes (AAA)")
    void slugRules(String title, String expectedStem) {
      // Act
      var name = titled(title.trim(), ReportFormat.PDF, reportFile()).suggestedFileName();

      // Assert
      assertThat(name).isEqualTo(expectedStem.trim() + ".pdf");
    }

    @Test
    @DisplayName("suggestedFileName() with a title that slugs to nothing -> report-<id> (AAA)")
    void emptyStemFallsBackToId() {
      // Act & Assert
      assertThat(titled("¡¡¡???", ReportFormat.PDF, reportFile()).suggestedFileName()).isEqualTo("report-500.pdf");
    }

    @Test
    @DisplayName("suggestedFileName() with a very long title -> stem cut to 100 characters without trailing dash (AAA)")
    void longStemIsCut() {
      // Arrange
      var title = "a".repeat(99) + " bbbbbbbbbb";

      // Act
      var name = titled(title, ReportFormat.PDF, reportFile()).suggestedFileName();

      // Assert
      assertThat(name).isEqualTo("a".repeat(99) + ".pdf");
      assertThat(titled("x".repeat(150), ReportFormat.PDF, reportFile()).suggestedFileName())
          .isEqualTo("x".repeat(100) + ".pdf");
    }

    @Test
    @DisplayName("suggestedFileName() -> extension comes from the stored key, lower-cased (AAA)")
    void extensionFromKey() {
      // Act & Assert
      assertThat(titled("t", ReportFormat.PDF, new ReportFile("reports/5/500.XLSX", "x", 1)).suggestedFileName())
          .isEqualTo("t.xlsx");
    }

    @Test
    @DisplayName("suggestedFileName() with a key lacking an extension -> falls back to the format name (AAA)")
    void extensionFallsBackToFormat() {
      // Act & Assert
      assertThat(titled("t", ReportFormat.CSV, new ReportFile("reports/5/500", "x", 1)).suggestedFileName())
          .isEqualTo("t.csv");
      assertThat(titled("t", ReportFormat.CSV, new ReportFile("reports/5/500.", "x", 1)).suggestedFileName())
          .isEqualTo("t.csv");
      assertThat(titled("t", ReportFormat.XLSX, new ReportFile("my.dir/500", "x", 1)).suggestedFileName())
          .isEqualTo("t.xlsx");
    }

    @Test
    @DisplayName("suggestedFileName() without a file -> IllegalStateException (AAA)")
    void withoutFile() {
      // Act & Assert
      assertThatThrownBy(() -> pendingReport().suggestedFileName())
          .isInstanceOf(IllegalStateException.class).hasMessage("Report 500 has no file to name");
      assertThat(FILE_KEY).endsWith(".pdf");
    }
  }
}
