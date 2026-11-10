package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.MaintenanceWindowResource;

/**
 * Maps a {@link MaintenanceWindow} into a {@link MaintenanceWindowResource}.
 */
public final class MaintenanceWindowResourceFromEntityAssembler {

  private MaintenanceWindowResourceFromEntityAssembler() {
  }

  /**
   * @param window the window
   * @return its REST representation
   */
  public static MaintenanceWindowResource toResourceFromEntity(MaintenanceWindow window) {
    return new MaintenanceWindowResource(
        window.getId(),
        window.getEquipmentId(),
        window.getOrigin().name(),
        window.getStartedAt(),
        window.getExpiresAt(),
        window.getEndedAt(),
        window.getClosureReason() == null ? null : window.getClosureReason().name(),
        window.getTechnicianProfileId(),
        window.getServiceRequestId(),
        window.isUnlinked(),
        window.isActive());
  }
}
