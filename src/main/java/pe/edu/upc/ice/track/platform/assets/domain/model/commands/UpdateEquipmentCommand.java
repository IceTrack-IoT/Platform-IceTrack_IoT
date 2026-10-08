package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;

/**
 * Command to replace the descriptive data and maintenance interval of an existing unit.
 *
 * <p>The threshold is excluded on purpose: it has {@link ChangeThresholdCommand} of its own so
 * that changing it always publishes a {@code TemperatureThresholdUpdatedEvent}.</p>
 *
 * @param equipmentId          identifier of the unit to update; required
 * @param ownerId              identifier of the owner the unit must belong to; required
 * @param name                 the new unit name; required
 * @param equipmentType        the new kind of refrigeration unit; required
 * @param reminderIntervalDays the new preventive maintenance interval in days; required and positive
 */
public record UpdateEquipmentCommand(
    Long equipmentId,
    Long ownerId,
    String name,
    EquipmentType equipmentType,
    Integer reminderIntervalDays) {
}