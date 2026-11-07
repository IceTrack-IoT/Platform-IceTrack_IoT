package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request for an operator-controlled device lifecycle transition.
 *
 * <p>{@code REVOKED} invalidates the API key at once while the board stays linked to its unit;
 * {@code UNPAIRED} unlinks the board and invalidates its key. {@code PAIRED} is only accepted for
 * a board that is already paired: a revoked board resumes through
 * {@code POST /api/v1/devices/{deviceId}/rotate-api-key}, which issues a new key.</p>
 */
@Schema(
    name = "ChangeDeviceStatusRequest",
    description = "Request for an operator-controlled device lifecycle transition",
    example = "{\"new_status\": \"REVOKED\"}"
)
public record ChangeDeviceStatusResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Status to move the device to. REVOKED invalidates the API key, UNPAIRED "
        + "also unlinks the equipment; a revoked device resumes only by rotating its API key",
        example = "REVOKED", allowableValues = {"UNPAIRED", "PAIRED", "REVOKED"})
    DeviceStatusResource newStatus) {
}
