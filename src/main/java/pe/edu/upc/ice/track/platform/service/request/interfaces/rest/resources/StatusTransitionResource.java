package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing one entry of the status history of a service request.
 */
@Schema(
    name = "StatusTransitionResponse",
    description = "One entry of the status history of a service request",
    example = "{\"from_status\": \"PENDING\", \"to_status\": \"ACCEPTED\", \"actor_id\": 7, \"occurred_at\": \"2026-10-23T13:40:00Z\", \"reason\": null, \"technician_id\": 7}")
public record StatusTransitionResource(
    @Schema(description = "Status before the transition; null for the creation", example = "PENDING", nullable = true)
    ServiceStatusResource fromStatus,

    @Schema(description = "Status after the transition", example = "ACCEPTED")
    ServiceStatusResource toStatus,

    @Schema(description = "Profile identifier of the owner or technician who acted", example = "7")
    Long actorId,

    @Schema(description = "When the transition happened, ISO-8601 UTC", example = "2026-10-23T13:40:00Z")
    String occurredAt,

    @Schema(description = "Why it happened, for a rejection or a cancellation", nullable = true)
    String reason,

    @Schema(description = "Technician the transition concerns", example = "7", nullable = true)
    Long technicianId
) {
}
