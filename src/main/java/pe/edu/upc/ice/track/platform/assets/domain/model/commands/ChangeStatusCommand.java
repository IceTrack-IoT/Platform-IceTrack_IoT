package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

/**
 * Command to move a unit to a new operational status.
 *
 * @param equipmentId identifier of the unit whose status changes; required
 * @param ownerId     identifier of the owner the unit must belong to; required
 * @param newStatus   the status to move to; required and part of the transition matrix
 */
public record ChangeStatusCommand(
    Long equipmentId,
    Long ownerId,
    StatusEquipment newStatus) {
}