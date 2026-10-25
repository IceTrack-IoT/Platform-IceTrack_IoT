package pe.edu.upc.ice.track.platform.reporting.application.internal.renderers;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.time.LocalDateTime;
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
 */
public record ReportContent(
    String title,
    ReportType type,
    ReportFilters filters,
    LocalDateTime generatedAt,
    List<KpiValue> kpis) {
}
