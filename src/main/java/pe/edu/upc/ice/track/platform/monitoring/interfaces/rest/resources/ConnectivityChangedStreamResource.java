package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Data of the {@code connectivity-changed} stream event: the connectivity of one of the stream owner's
 * units changed. Offline detection found it silent, the next batch brought it back, it was deactivated
 * or reactivated, or a maintenance window opened or closed on it (then the state may stay the same).
 */
@Schema(
    name = "ConnectivityChangedStreamEvent",
    description = "Data of the connectivity-changed event",
    example = "{\"equipment_id\": 10, \"device_id\": 7, \"previous_state\": \"REPORTING\", \"state\": \"SILENT\", "
        + "\"last_received_at\": \"2026-11-09T14:51:12Z\", \"under_maintenance\": false, "
        + "\"changed_at\": \"2026-11-09T14:55:00Z\"}")
public record ConnectivityChangedStreamResource(
    @Schema(description = "Identifier of the unit", example = "10")
    Long equipmentId,

    @Schema(description = "Board linked to the unit; null when none is", example = "7", nullable = true)
    Long deviceId,

    @Schema(description = "State before the change: REPORTING, SILENT, NEVER_REPORTED or NOT_MONITORED",
        example = "REPORTING")
    String previousState,

    @Schema(description = "State after the change, same values", example = "SILENT")
    String state,

    @Schema(description = "Server arrival time of the unit's latest batch", example = "2026-11-09T14:51:12Z",
        nullable = true)
    Instant lastReceivedAt,

    @Schema(description = "Whether a maintenance window covers the unit after the change", example = "false")
    boolean underMaintenance,

    @Schema(description = "When the change happened", example = "2026-11-09T14:55:00Z")
    Instant changedAt
) {
}
