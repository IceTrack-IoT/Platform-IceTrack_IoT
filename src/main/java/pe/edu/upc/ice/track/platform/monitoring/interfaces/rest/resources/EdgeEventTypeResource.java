package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;

/**
 * REST mirror of {@link EdgeEventType}.
 */
@Schema(name = "EdgeEventType", description = "Kind of event reported by an edge gateway",
    example = "THERMAL_EXCURSION_DETECTED")
public enum EdgeEventTypeResource {
  THERMAL_EXCURSION_DETECTED,
  THERMAL_EXCURSION_NORMALIZED;

  /**
   * @return the matching {@link EdgeEventType}
   */
  public EdgeEventType toDomain() {
    return EdgeEventType.valueOf(name());
  }
}
