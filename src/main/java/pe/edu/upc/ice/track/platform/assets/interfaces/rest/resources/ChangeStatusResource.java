package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Resource received to move a unit to a new operational status.
 *
 * <p>Whether the requested move is actually allowed is decided by the transition matrix carried by
 * {@code StatusEquipment}, not here: a rejected transition is a 409, not a validation failure,
 * because the payload itself is well-formed.</p>
 */
@Schema(
    name = "ChangeStatusRequest",
    description = "Equipment status change request",
    example = "{\"new_status\": \"MAINTENANCE\"}")
public record ChangeStatusResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(
        description = "Status to move to. Allowed transitions: ACTIVE to MAINTENANCE/REPAIR/DEACTIVATED, "
            + "MAINTENANCE to ACTIVE/REPAIR/DEACTIVATED, REPAIR to ACTIVE/DEACTIVATED, "
            + "DEACTIVATED to ACTIVE",
        example = "MAINTENANCE")
    StatusEquipmentResource newStatus
) {
}