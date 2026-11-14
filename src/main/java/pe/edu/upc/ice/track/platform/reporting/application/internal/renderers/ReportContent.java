package pe.edu.upc.ice.track.platform.reporting.application.internal.renderers;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.time.Instant;
import java.util.List;

/**
 * Everything a {@link ReportRenderer} needs to lay out a report, gathered into one immutable
 * input so renderers depend on neither the {@code Report} aggregate nor its lifecycle.
 *
 * @param title       the report's display title
 * @param type        the kind of report
 * @param filters     the scope the KPIs were computed over
 * @param generatedAt when the KPIs were computed
 * @param kpis        the computed KPIs, in display order
 * @param sections    the detail blocks shown after the KPIs, in display order; empty for a report
 *                    made of KPIs only
 */
public record ReportContent(
    String title,
    ReportType type,
    ReportFilters filters,
    Instant generatedAt,
    List<KpiValue> kpis,
    List<ReportSection> sections) {

  /**
   * Keeps the sections unmodifiable.
   */
  public ReportContent {
    sections = sections == null ? List.of() : List.copyOf(sections);
  }

  /**
   * Creates the content of a report made of KPIs only.
   */
  public ReportContent(String title, ReportType type, ReportFilters filters, Instant generatedAt, List<KpiValue> kpis) {
    this(title, type, filters, generatedAt, kpis, List.of());
  }
}
