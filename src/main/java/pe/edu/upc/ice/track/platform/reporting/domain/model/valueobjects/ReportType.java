package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

/**
 *  Enumeration representing the kind of report being requested.
 *
 *  <p>Each constant is handled by exactly one {@code ReportGenerationStrategy} bean, keyed by
 *  this enum's name -- see {@code ReportCommandServiceImpl} for how the two are wired.</p>
 */
public enum ReportType {
  MAINTENANCE_COMPLIANCE,
  EQUIPMENT_UPTIME,
  TECHNICIAN_PERFORMANCE,
  TEMPERATURE_EXCURSION
}
