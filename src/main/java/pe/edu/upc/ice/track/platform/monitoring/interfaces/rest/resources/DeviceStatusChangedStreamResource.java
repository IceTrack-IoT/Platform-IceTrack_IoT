package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Data of the {@code device-status-changed} stream event: a board of the stream owner was paired, had
 * its credential rotated or revoked, or was unpaired, from whatever session. A rotation keeps the board
 * {@code PAIRED}, so both statuses may be equal.
 */
@Schema(
    name = "DeviceStatusChangedStreamEvent",
    description = "Data of the device-status-changed event",
    example = "{\"device_id\": 7, \"paired_equipment_id\": 21, \"previous_status\": \"PAIRED\", \"status\": \"REVOKED\", "
        + "\"changed_at\": \"2026-11-09T10:02:01Z\"}")
public record DeviceStatusChangedStreamResource(
    @Schema(description = "Identifier of the board", example = "7")
    Long deviceId,

    @Schema(description = "Unit the board is linked to after the change; null once unpaired", example = "21",
        nullable = true)
    Long pairedEquipmentId,

    @Schema(description = "Status before the change: UNPAIRED, PAIRED or REVOKED", example = "PAIRED")
    String previousStatus,

    @Schema(description = "Status after the change: UNPAIRED, PAIRED or REVOKED", example = "REVOKED")
    String status,

    @Schema(description = "When the change happened", example = "2026-11-09T10:02:01Z")
    Instant changedAt
) {
}
