package pe.edu.upc.ice.track.platform.reporting.application.internal.renderers;

import java.util.List;

/**
 * A titled block of label and value rows that a report shows after its KPIs.
 *
 * <p>Used by reports that document one thing in detail rather than measure a period, such as the
 * service document of a completed request: its status history, the intervention and the review each
 * become one section. Every value is already formatted text.</p>
 *
 * @param title the heading of the section
 * @param rows  the rows, in display order
 */
public record ReportSection(String title, List<Row> rows) {

  /**
   * Keeps the rows unmodifiable.
   */
  public ReportSection {
    rows = rows == null ? List.of() : List.copyOf(rows);
  }

  /**
   * One row of a section.
   *
   * @param label what the value is
   * @param value the value, already formatted; {@code null} is shown as empty
   */
  public record Row(String label, String value) {
  }
}
