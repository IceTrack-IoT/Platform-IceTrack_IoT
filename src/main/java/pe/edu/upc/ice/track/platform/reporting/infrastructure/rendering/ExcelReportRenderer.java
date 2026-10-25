package pe.edu.upc.ice.track.platform.reporting.infrastructure.rendering;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportRenderer;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Realizes {@link ReportRenderer} for {@code ReportFormat.EXCEL} using Apache POI.
 *
 * <p>Produces an {@code .xlsx} workbook with one sheet: a metadata block followed by a KPI table
 * whose value column holds real numbers (not text), so the user can sum, chart or filter them
 * directly in Excel. Text cells are written as strings, so a user-supplied title is never
 * interpreted as a formula.</p>
 */
@Service("EXCEL")
public class ExcelReportRenderer implements ReportRenderer {

  @Override
  public byte[] render(ReportContent content) {
    try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Report");

      Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      CellStyle boldStyle = workbook.createCellStyle();
      boldStyle.setFont(boldFont);

      int rowIndex = 0;
      rowIndex = metaRow(sheet, rowIndex, "Report", content.title(), boldStyle);
      rowIndex = metaRow(sheet, rowIndex, "Type", content.type().name(), boldStyle);
      rowIndex = metaRow(sheet, rowIndex, "Generated at", content.generatedAt().toString(), boldStyle);
      rowIndex = metaRow(sheet, rowIndex, "From", content.filters().dateRange().from().toString(), boldStyle);
      rowIndex = metaRow(sheet, rowIndex, "To", content.filters().dateRange().to().toString(), boldStyle);
      rowIndex++;

      Row header = sheet.createRow(rowIndex++);
      String[] titles = {"KPI", "Value", "Unit"};
      for (int i = 0; i < titles.length; i++) {
        var cell = header.createCell(i);
        cell.setCellValue(titles[i]);
        cell.setCellStyle(boldStyle);
      }

      for (KpiValue kpi : content.kpis()) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(kpi.label());
        row.createCell(1).setCellValue(kpi.value());
        row.createCell(2).setCellValue(kpi.unit());
      }

      for (int i = 0; i < 3; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Could not render the report as Excel", ex);
    }
  }

  @Override
  public String contentType() {
    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  }

  @Override
  public String fileExtension() {
    return "xlsx";
  }

  private static int metaRow(Sheet sheet, int rowIndex, String label, String value, CellStyle labelStyle) {
    Row row = sheet.createRow(rowIndex);
    var labelCell = row.createCell(0);
    labelCell.setCellValue(label);
    labelCell.setCellStyle(labelStyle);
    row.createCell(1).setCellValue(value);
    return rowIndex + 1;
  }
}
