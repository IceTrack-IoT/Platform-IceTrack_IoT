package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;

/**
 * REST representation of the generation status of a report, accepted by the report listing filters.
 */
@Schema(name = "ReportStatus", description = "Generation status of a report", example = "COMPLETED")
public enum ReportStatusResource {

  PENDING,
  GENERATING,
  COMPLETED,
  FAILED;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain status
   */
  public ReportStatus toDomain() {
    return ReportStatus.valueOf(name());
  }
}
