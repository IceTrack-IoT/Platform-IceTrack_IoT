package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing a maintenance window opened on the unit while the work of a
 * service request was in progress.
 */
@Schema(
    name = "ServiceRequestMaintenanceWindowResponse",
    description = "Maintenance window linked to a service request",
    example = "{\"window_id\": 18, \"origin\": \"APP\", \"technician_id\": 7, \"started_at\": \"2026-10-23T14:10:00Z\", "
        + "\"ended_at\": \"2026-10-23T15:20:00Z\", \"closure_reason\": \"INTERVENTION_COMPLETED\"}")
public record MaintenanceWindowLinkResource(
    @Schema(description = "Maintenance window identifier, as listed by GET /api/v1/maintenance-windows", example = "18")
    Long windowId,

    @Schema(description = "How the window was started", example = "APP", allowableValues = {"APP", "BUTTON"})
    String origin,

    @Schema(description = "Technician attributed to the window; null when unknown", example = "7", nullable = true)
    Long technicianId,

    @Schema(description = "When the window started, ISO-8601 UTC", example = "2026-10-23T14:10:00Z")
    String startedAt,

    @Schema(description = "When the window ended; null while it is open", example = "2026-10-23T15:20:00Z",
        nullable = true)
    String endedAt,

    @Schema(description = "Why the window ended; null while it is open", example = "INTERVENTION_COMPLETED",
        nullable = true)
    String closureReason
) {
}
