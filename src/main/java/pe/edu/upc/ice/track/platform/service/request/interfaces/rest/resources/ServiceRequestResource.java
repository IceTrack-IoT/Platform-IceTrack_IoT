package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource representing a service request, with its status history and, once
 * completed, its intervention.
 */
@Schema(
    name = "ServiceRequestResponse",
    description = "Service request with its status history",
    example = "{\"id\": 42, \"owner_id\": 3, \"equipment_id\": 10, \"site_id\": 1, \"technician_id\": 7, "
        + "\"type\": \"REPAIR\", \"priority\": \"HIGH\", \"description\": \"The freezer does not reach its temperature\", "
        + "\"status\": \"ACCEPTED\", \"created_at\": \"2026-10-23T13:00:00Z\", \"assigned_at\": \"2026-10-23T13:10:00Z\", "
        + "\"started_at\": null, \"completed_at\": null, \"canceled_at\": null, \"cancel_reason\": null, "
        + "\"history\": [], \"intervention\": null}")
public record ServiceRequestResource(
    @Schema(description = "Service request unique identifier", example = "42")
    Long id,

    @Schema(description = "Owner who created the request", example = "3")
    Long ownerId,

    @Schema(description = "Unit to service", example = "10")
    Long equipmentId,

    @Schema(description = "Site the unit is installed at", example = "1")
    Long siteId,

    @Schema(description = "Assigned technician; null while none is assigned", example = "7", nullable = true)
    Long technicianId,

    @Schema(description = "Kind of work", example = "REPAIR")
    ServiceTypeResource type,

    @Schema(description = "Urgency", example = "HIGH")
    ServicePriorityResource priority,

    @Schema(description = "What the owner needs", example = "The freezer does not reach its temperature")
    String description,

    @Schema(description = "Current lifecycle status", example = "ACCEPTED")
    ServiceStatusResource status,

    @Schema(description = "When the request was created, ISO-8601 UTC", example = "2026-10-23T13:00:00Z")
    String createdAt,

    @Schema(description = "When the current technician was assigned", example = "2026-10-23T13:10:00Z", nullable = true)
    String assignedAt,

    @Schema(description = "When the work started", nullable = true)
    String startedAt,

    @Schema(description = "When the work finished", nullable = true)
    String completedAt,

    @Schema(description = "When the owner canceled", nullable = true)
    String canceledAt,

    @Schema(description = "Why the owner canceled", nullable = true)
    String cancelReason,

    @Schema(description = "Status history, oldest first; the first entry is the creation")
    List<StatusTransitionResource> history,

    @Schema(description = "The intervention that completed the request; null until completed", nullable = true)
    InterventionResource intervention
) {
}
