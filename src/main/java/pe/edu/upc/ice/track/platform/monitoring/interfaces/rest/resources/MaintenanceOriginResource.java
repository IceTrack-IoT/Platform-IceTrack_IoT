package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

/**
 * REST mirror of {@link MaintenanceOrigin}.
 */
@Schema(name = "MaintenanceOrigin", description = "How a maintenance window was started: APP (2 hours) or "
    + "BUTTON (30 minutes)", example = "APP")
public enum MaintenanceOriginResource {
  APP,
  BUTTON;

  /**
   * @return the matching {@link MaintenanceOrigin}
   */
  public MaintenanceOrigin toDomain() {
    return MaintenanceOrigin.valueOf(name());
  }
}
