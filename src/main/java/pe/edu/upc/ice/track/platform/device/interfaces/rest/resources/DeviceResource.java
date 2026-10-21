package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe REST representation of a managed device; it intentionally omits the API-key hash. */
@Schema(name = "DeviceResponse")
public record DeviceResource(
    Long id,
    String deviceUid,
    String boardModel,
    String firmwareVersion,
    DeviceStatusResource status,
    Long pairedEquipmentId,
    String lastSeenAt) {
}
