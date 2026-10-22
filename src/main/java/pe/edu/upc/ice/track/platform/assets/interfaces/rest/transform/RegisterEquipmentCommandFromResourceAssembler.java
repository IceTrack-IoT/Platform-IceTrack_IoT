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
   * @param username username of the authenticated caller
   * @param resource the {@link RegisterEquipmentResource} payload
   * @return the {@link RegisterEquipmentCommand} command
   */
  public static RegisterEquipmentCommand toCommandFromResource(String username, RegisterEquipmentResource resource) {
    return new RegisterEquipmentCommand(
        username,
        resource.siteId(),
        resource.uid(),
        resource.name(),
        resource.equipmentType().toDomain(),
        resource.minCelsius(),
        resource.maxCelsius(),
        resource.reminderIntervalDays());
  }
}
