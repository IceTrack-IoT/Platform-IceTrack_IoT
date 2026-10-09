package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateEquipmentResource;

/**
 * Assembler that converts an {@link UpdateEquipmentResource} into an {@link UpdateEquipmentCommand}.
 */
public class UpdateEquipmentCommandFromResourceAssembler {

  /**
   * Converts the equipment update payload into its command representation.
   *
   * @param equipmentId identifier of the unit being updated, taken from the request path
   * @param ownerId identifier of the authenticated owner
   * @param resource the {@link UpdateEquipmentResource} payload
   * @return the {@link UpdateEquipmentCommand} command
   */
  public static UpdateEquipmentCommand toCommandFromResource(
      Long equipmentId, Long ownerId, UpdateEquipmentResource resource) {
    return new UpdateEquipmentCommand(
        equipmentId,
        ownerId,
        resource.name(),
        resource.equipmentType().toDomain(),
        resource.reminderIntervalDays());
  }
}
