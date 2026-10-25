package pe.edu.upc.ice.track.platform.reporting.infrastructure.rendering;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportRenderer;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Realizes {@link ReportRenderer} for {@code ReportFormat.CSV}.
 *
 * <p>Output is UTF-8 with a byte-order mark so Excel opens accented characters correctly, and
 * numbers are formatted with {@link Locale#ROOT} (dot decimal separator) so the file does not
 * change with the server's locale. Every text cell is escaped against CSV structure
 * (quotes, commas, line breaks) and against formula injection: a value starting with
 * {@code =}, {@code +}, {@code -} or {@code @} gets a leading apostrophe, because the report
 * title is user-supplied and spreadsheets would otherwise execute it as a formula.</p>
 */
@Service("CSV")
public class CsvReportRenderer implements ReportRenderer {

  private static final String BOM = "﻿";
  private static final String NEWLINE = "\r\n";

  @Override
  public byte[] render(ReportContent content) {
    var sb = new StringBuilder(BOM);
    row(sb, "Report", content.title());
    row(sb, "Type", content.type().name());
    row(sb, "Generated at", content.generatedAt().toString());
    row(sb, "From", content.filters().dateRange().from().toString());
    row(sb, "To", content.filters().dateRange().to().toString());
    sb.append(NEWLINE);
    row(sb, "KPI", "Value", "Unit");
    for (KpiValue kpi : content.kpis()) {
      row(sb, kpi.label(), String.format(Locale.ROOT, "%.2f", kpi.value()), kpi.unit());
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String contentType() {
    return "text/csv";
  }

  @Override
  public String fileExtension() {
    return "csv";
  }

  private static void row(StringBuilder sb, String... cells) {
    for (int i = 0; i < cells.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escape(cells[i]));
    }
    sb.append(NEWLINE);
  }

  private static String escape(String raw) {
    String value = raw == null ? "" : raw;
    if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
      value = "'" + value;
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
