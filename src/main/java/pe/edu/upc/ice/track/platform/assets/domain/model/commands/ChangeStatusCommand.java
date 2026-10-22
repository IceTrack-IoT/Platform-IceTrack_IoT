package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

/**
 * Command to move a unit to a new operational status.
 *
 * <p>The only write open to a technician, who changes the status of the units it services in the
 * field whatever their owner.</p>
 *
 * @param equipmentId identifier of the unit whose status changes; required
 * @param username    username of the authenticated caller, resolved by the command service to the
 *                    owner the unit must belong to when the caller acts as an owner; required
 * @param callerRole  the role the caller acts in; {@code null} is treated as
 *                    {@link CallerRole#OWNER}
 * @param newStatus   the status to move to; required and part of the transition matrix
 */
public record ChangeStatusCommand(
    Long equipmentId,
    String username,
    CallerRole callerRole,
    StatusEquipment newStatus) {
}