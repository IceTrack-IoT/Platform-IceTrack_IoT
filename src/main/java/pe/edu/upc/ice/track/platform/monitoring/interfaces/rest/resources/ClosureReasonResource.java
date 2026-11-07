package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;

/**
 * REST mirror of {@link ClosureReason}.
 */
@Schema(name = "MaintenanceClosureReason", description = "Why a maintenance window ended", example = "MANUAL_APP")
public enum ClosureReasonResource {
  MANUAL_APP,
  MANUAL_BUTTON,
  INTERVENTION_COMPLETED,
  EXPIRED,
  SAFETY_LIMIT;

  /**
   * @return the matching {@link ClosureReason}
   */
  public ClosureReason toDomain() {
    return ClosureReason.valueOf(name());
  }
}
