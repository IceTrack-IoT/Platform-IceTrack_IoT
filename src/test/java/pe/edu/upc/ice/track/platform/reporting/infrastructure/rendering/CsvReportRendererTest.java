package pe.edu.upc.ice.track.platform.reporting.infrastructure.rendering;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportSection;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

class CsvReportRendererTest {

  private static final Instant GENERATED = Instant.parse("2026-02-01T10:00:00Z");
  private final CsvReportRenderer renderer = new CsvReportRenderer();

  private String render(String title, List<KpiValue> kpis, List<ReportSection> sections) {
    var content = new ReportContent(title, ReportType.EQUIPMENT_UPTIME, siteFilters(), GENERATED, kpis, sections);
    return new String(renderer.render(content), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("contentType() / fileExtension() -> text/csv and csv (AAA)")
  void metadata() {
    // Act & Assert
    assertThat(renderer.contentType()).isEqualTo("text/csv");
    assertThat(renderer.fileExtension()).isEqualTo("csv");
  }

  @Test
  @DisplayName("render(content) -> BOM, CRLF header rows, KPI table with two decimals (AAA)")
  void layout() {
    // Arrange
    var kpis = List.of(new KpiValue("k", "Average uptime", 87.456, "%"), new KpiValue("c", "Units", 3.0, "count"));

    // Act
    var csv = render("Monthly", kpis, List.of());

    // Assert
    assertThat(csv).isEqualTo("﻿"
        + "Report,Monthly\r\n"
        + "Type,EQUIPMENT_UPTIME\r\n"
        + "Generated at,2026-02-01T10:00:00Z\r\n"
        + "From,2026-01-01T00:00:00Z\r\n"
        + "To,2026-01-02T00:00:00Z\r\n"
        + "\r\n"
        + "KPI,Value,Unit\r\n"
        + "Average uptime,87.46,%\r\n"
        + "Units,3.00,count\r\n");
  }

  @Test
  @DisplayName("render(content) -> sections appended after a blank line with title and label/value rows (AAA)")
  void sections() {
    // Arrange
    var section = new ReportSection("Review", List.of(new ReportSection.Row("Average", "4.00 / 5")));

    // Act
    var csv = render("T", List.of(), List.of(section));

    // Assert
    assertThat(csv).endsWith("KPI,Value,Unit\r\n\r\nReview\r\nAverage,4.00 / 5\r\n");
  }

  @Test
  @DisplayName("render(content) -> cells starting with = + - @ are prefixed with a quote against CSV injection (AAA)")
  void injectionGuard() {
    // Act
    var csv = render("=HYPERLINK(\"x\")", List.of(new KpiValue("k", "+cmd", 1.0, "@u"), new KpiValue("k2", "-1", 2.0, "u")), List.of());

    // Assert
    assertThat(csv).contains("Report,\"'=HYPERLINK(\"\"x\"\")\"\r\n");
    assertThat(csv).contains("'+cmd,1.00,'@u\r\n");
    assertThat(csv).contains("'-1,2.00,u\r\n");
  }

  @Test
  @DisplayName("render(content) -> commas, quotes and line breaks are quoted with doubled quotes (AAA)")
  void escaping() {
    // Arrange
    var section = new ReportSection("S", List.of(
        new ReportSection.Row("a,b", "say \"hi\""),
        new ReportSection.Row("multi", "line1\nline2"),
        new ReportSection.Row("empty", null)));

    // Act
    var csv = render("T", List.of(), List.of(section));

    // Assert
    assertThat(csv).contains("\"a,b\",\"say \"\"hi\"\"\"\r\n");
    assertThat(csv).contains("multi,\"line1\nline2\"\r\n");
    assertThat(csv).contains("empty,\r\n");
  }

  @Test
  @DisplayName("render(content) -> non-ASCII text is written as UTF-8 (AAA)")
  void utf8() {
    // Act
    var bytes = renderer.render(new ReportContent("Mantención", ReportType.EQUIPMENT_UPTIME, siteFilters(), GENERATED, List.of()));

    // Assert
    assertThat(new String(bytes, StandardCharsets.UTF_8)).contains("Report,Mantención");
  }
}
