package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Connectivity of a unit, evaluated by the server at read time with the same rule as the scheduled
 * offline detection.
 *
 * <p>To keep it current between reads without trusting its own clock, a client computes
 * {@code skew = evaluated_at - now} when it receives the resource, and compares
 * {@code last_received_at + silent_after_seconds} with {@code now + skew}.</p>
 *
 * @param state              the evaluated state
 * @param lastReceivedAt     server arrival time of the latest stored batch; null when NEVER_REPORTED
 * @param silentAfterSeconds the offline window of the unit's alert policy, in seconds
 * @param silentSince        {@code last_received_at + silent_after_seconds}; present only when SILENT
 * @param underMaintenance   whether a maintenance window covers the evaluation time
 * @param evaluatedAt        server time of the evaluation
 */
@Schema(
    name = "EquipmentConnectivity",
    description = "Connectivity of a unit, evaluated by the server at read time",
    example = "{\"state\": \"SILENT\", \"last_received_at\": \"2026-11-09T14:51:12Z\", \"silent_after_seconds\": 180, "
        + "\"silent_since\": \"2026-11-09T14:54:12Z\", \"under_maintenance\": false, "
        + "\"evaluated_at\": \"2026-11-09T15:02:40Z\"}")
public record EquipmentConnectivityResource(
    @Schema(description = "Evaluated state", example = "SILENT")
    ConnectivityStateResource state,

    @Schema(description = "Server arrival time of the latest stored batch; null when NEVER_REPORTED",
        example = "2026-11-09T14:51:12Z", nullable = true)
    Instant lastReceivedAt,

    @Schema(description = "Offline window of the unit's alert policy: 60 x missed_sync_windows_for_offline",
        example = "180")
    long silentAfterSeconds,

    @Schema(description = "last_received_at + silent_after_seconds; present only when SILENT",
        example = "2026-11-09T14:54:12Z", nullable = true)
    Instant silentSince,

    @Schema(description = "A maintenance window covers the evaluation time; offline detection skips such units",
        example = "false")
    boolean underMaintenance,

    @Schema(description = "Server time of the evaluation", example = "2026-11-09T15:02:40Z")
    Instant evaluatedAt
) {
}
