package pe.edu.upc.ice.track.platform.reporting.infrastructure.rendering;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportRenderer;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Realizes {@link ReportRenderer} for {@code ReportFormat.PDF} using OpenPDF.
 *
 * <p>Lays the report out as a title, a short block of metadata (type, generation time, date
 * range) and a three-column table of KPIs (name, value, unit).</p>
 */
@Service("PDF")
public class PdfReportRenderer implements ReportRenderer {

  @Override
  public byte[] render(ReportContent content) {
    try {
      var out = new ByteArrayOutputStream();
      var document = new Document(PageSize.A4, 50, 50, 50, 50);
      PdfWriter.getInstance(document, out);
      document.open();

      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
      Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
      Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
      Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

      document.add(new Paragraph(content.title(), titleFont));
      document.add(new Paragraph("Type: " + content.type().name(), metaFont));
      document.add(new Paragraph("Generated at: " + content.generatedAt(), metaFont));
      document.add(new Paragraph(
          "Period: " + content.filters().dateRange().from() + " - " + content.filters().dateRange().to(),
          metaFont));
      document.add(new Paragraph(" "));

      var table = new PdfPTable(new float[] {4f, 1.5f, 1f});
      table.setWidthPercentage(100);
      headerCell(table, "KPI", headerFont);
      headerCell(table, "Value", headerFont);
      headerCell(table, "Unit", headerFont);
      for (KpiValue kpi : content.kpis()) {
        table.addCell(new PdfPCell(new Phrase(kpi.label(), cellFont)));
        var valueCell = new PdfPCell(new Phrase(String.format(Locale.ROOT, "%.2f", kpi.value()), cellFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
        table.addCell(new PdfPCell(new Phrase(kpi.unit(), cellFont)));
      }
      document.add(table);

      document.close();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Could not render the report as PDF", ex);
    }
  }

  @Override
  public String contentType() {
    return "application/pdf";
  }

  @Override
  public String fileExtension() {
    return "pdf";
  }

  private static void headerCell(PdfPTable table, String text, Font font) {
    var cell = new PdfPCell(new Phrase(text, font));
    cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
    table.addCell(cell);
  }
}
