package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.MaintenanceWindowPersistenceEntity;

/** Bidirectional mapper between {@link MaintenanceWindow} and {@link MaintenanceWindowPersistenceEntity}. */
public final class MaintenanceWindowPersistenceAssembler {

  private MaintenanceWindowPersistenceAssembler() {
  }

  /**
   * Copies the state of a window onto an entity, which is either new or the managed entity the
   * window was loaded from.
   *
   * @param window the window
   * @param entity the entity to update
   */
  public static void copyToPersistence(MaintenanceWindow window, MaintenanceWindowPersistenceEntity entity) {
    entity.setStartEventUid(window.getStartEventUid());
    entity.setEquipmentId(window.getEquipmentId());
    entity.setOrigin(window.getOrigin());
    entity.setStartedAt(window.getStartedAt());
    entity.setExpiresAt(window.getExpiresAt());
    entity.setEndedAt(window.getEndedAt());
    entity.setClosureReason(window.getClosureReason());
    entity.setTechnicianProfileId(window.getTechnicianProfileId());
    entity.setServiceRequestId(window.getServiceRequestId());
  }

  /**
   * @param entity the persisted row
   * @return the window it holds
   */
  public static MaintenanceWindow toDomainFromPersistenceEntity(MaintenanceWindowPersistenceEntity entity) {
    return new MaintenanceWindow(
        entity.getId(),
        entity.getStartEventUid(),
        entity.getEquipmentId(),
        entity.getOrigin(),
        entity.getStartedAt(),
        entity.getExpiresAt(),
        entity.getEndedAt(),
        entity.getClosureReason(),
        entity.getTechnicianProfileId(),
        entity.getServiceRequestId());
  }
}
