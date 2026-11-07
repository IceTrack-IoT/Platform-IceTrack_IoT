package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Outbound REST resource representing a maintenance window of a unit.
 */
@Schema(
    name = "MaintenanceWindowResponse",
    description = "A maintenance window reported by the unit's gateway",
    example = "{\"id\": 18, \"equipment_id\": 10, \"origin\": \"APP\", \"started_at\": \"2026-11-07T13:05:00Z\", "
        + "\"expires_at\": \"2026-11-07T15:05:00Z\", \"ended_at\": \"2026-11-07T14:10:00Z\", "
        + "\"closure_reason\": \"MANUAL_APP\", \"technician_id\": 7, \"service_request_id\": 42, "
        + "\"unlinked\": false, \"active\": false}")
public record MaintenanceWindowResource(
    @Schema(description = "Maintenance window unique identifier", example = "18")
    Long id,

    @Schema(description = "Unit the window was opened on", example = "10")
    Long equipmentId,

    @Schema(description = "How the window was started, or promoted to", example = "APP",
        allowableValues = {"APP", "BUTTON"})
    String origin,

    @Schema(description = "When the gateway started the window, ISO-8601 UTC", example = "2026-11-07T13:05:00Z")
    Instant startedAt,

    @Schema(description = "When the window expires, or expired, unless closed earlier", example = "2026-11-07T15:05:00Z")
    Instant expiresAt,

    @Schema(description = "When the window ended; null while active", example = "2026-11-07T14:10:00Z", nullable = true)
    Instant endedAt,

    @Schema(description = "Why the window ended; null while active", example = "MANUAL_APP", nullable = true,
        allowableValues = {"MANUAL_APP", "MANUAL_BUTTON", "INTERVENTION_COMPLETED", "EXPIRED", "SAFETY_LIMIT"})
    String closureReason,

    @Schema(description = "Technician attributed to the window; null when unknown", example = "7", nullable = true)
    Long technicianId,

    @Schema(description = "Service request the window is linked to; null when unlinked", example = "42",
        nullable = true)
    Long serviceRequestId,

    @Schema(description = "Whether no service request is linked to the window", example = "false")
    boolean unlinked,

    @Schema(description = "Whether the window is still open", example = "false")
    boolean active
) {
}
