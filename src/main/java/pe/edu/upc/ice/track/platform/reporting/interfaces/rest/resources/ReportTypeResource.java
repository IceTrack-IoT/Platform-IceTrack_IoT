package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

/**
 * REST representation of the kind of a report, accepted by the report listing filters.
 */
@Schema(name = "ReportType", description = "Kind of report", example = "TEMPERATURE_EXCURSION")
public enum ReportTypeResource {

  MAINTENANCE_COMPLIANCE,
  EQUIPMENT_UPTIME,
  TECHNICIAN_PERFORMANCE,
  TEMPERATURE_EXCURSION,
  SERVICE_INTERVENTION;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain type
   */
  public ReportType toDomain() {
    return ReportType.valueOf(name());
  }
}
