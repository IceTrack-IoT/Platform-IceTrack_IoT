package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Resource received to assign a technician to a service request.
 *
 * <p>Whether the technician is ready to be assigned is decided by the {@code profiles} context, not
 * here: an ineligible technician is a 409, not a validation failure, because the payload itself is
 * well-formed.</p>
 */
@Schema(
    name = "AssignTechnicianRequest",
    description = "Technician assignment request",
    example = "{\"technician_profile_id\": 7}")
public record AssignTechnicianResource(
    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Identifier of the technician profile to assign", example = "7")
    Long technicianProfileId
) {
}
