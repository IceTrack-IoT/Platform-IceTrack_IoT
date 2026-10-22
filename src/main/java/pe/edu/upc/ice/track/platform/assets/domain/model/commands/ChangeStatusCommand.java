package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

/**
 * Command to move a unit to a new operational status.
 *
 * @param equipmentId identifier of the unit whose status changes; required
 * @param username    username of the authenticated caller, resolved by the command service to the
 *                    owner the unit must belong to; required
 * @param newStatus   the status to move to; required and part of the transition matrix
 */
public record ChangeStatusCommand(
    Long equipmentId,
    String username,
    StatusEquipment newStatus) {
}