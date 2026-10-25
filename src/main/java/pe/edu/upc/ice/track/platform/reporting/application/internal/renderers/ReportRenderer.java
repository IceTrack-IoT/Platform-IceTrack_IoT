package pe.edu.upc.ice.track.platform.reporting.application.internal.renderers;

/**
 * Strategy pattern contract for turning computed KPIs into a downloadable file.
 *
 * <p>It is the second Strategy of this context, next to {@code ReportGenerationStrategy}: that one
 * decides <em>what</em> is computed (by {@code ReportType}), this one decides <em>how it is
 * presented</em> (by {@code ReportFormat}). Each
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat} is
 * realized by one {@code @Service}-annotated implementation registered under a bean qualifier
 * equal to the format's enum name (e.g. {@code "PDF"}), and {@code ReportCommandServiceImpl}
 * resolves it from a {@code Map<String, ReportRenderer>}.</p>
 */
public interface ReportRenderer {

  /**
   * Lays out a report as file bytes.
   *
   * @param content the report to render; required
   * @return the file bytes
   */
  byte[] render(ReportContent content);

  /**
   * @return the MIME type of the files this renderer produces, e.g. {@code application/pdf}
   */
  String contentType();

  /**
   * @return the file extension, without the dot, e.g. {@code pdf}
   */
  String fileExtension();
}
