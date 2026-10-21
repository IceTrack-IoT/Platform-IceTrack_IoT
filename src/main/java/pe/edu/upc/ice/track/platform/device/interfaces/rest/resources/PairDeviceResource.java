package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Request that associates a provisioned board with an equipment owned by the caller. */
@Schema(name = "PairDeviceRequest", example = "{\"equipment_id\": 21}")
public record PairDeviceResource(
    @NotNull(message = "{validation.not-null}")
    Long equipmentId) {
}
