package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

/**
 *  Enumeration representing the export format a report was requested in.
 *
 *  <p>This context only computes and stores {@code resultPayload} as JSON; it does not render a
 *  PDF, Excel or CSV file itself. {@code format} is kept on the request so a future export step
 *  (or a client-side renderer) knows which output the user actually asked for.</p>
 */
public enum ReportFormat {
  PDF,
  EXCEL,
  CSV
}
