package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterEquipmentResource;

/**
 * Assembler that converts a {@link RegisterEquipmentResource} into a {@link RegisterEquipmentCommand}.
 */
public class RegisterEquipmentCommandFromResourceAssembler {

  /**
   * Converts the equipment registration payload into its command representation.
   *
   * @param ownerId identifier of the authenticated owner
   * @param resource the {@link RegisterEquipmentResource} payload
   * @return the {@link RegisterEquipmentCommand} command
   */
  public static RegisterEquipmentCommand toCommandFromResource(Long ownerId, RegisterEquipmentResource resource) {
    return new RegisterEquipmentCommand(
        ownerId,
        resource.siteId(),
        resource.uid(),
        resource.name(),
        resource.equipmentType().toDomain(),
        resource.minCelsius(),
        resource.maxCelsius(),
        resource.reminderIntervalDays());
  }
}
