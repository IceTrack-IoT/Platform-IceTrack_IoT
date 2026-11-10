package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing a service request in a list, without its status history,
 * intervention details or maintenance window links.
 *
 * <p>Returned by {@code GET /api/v1/service-requests?view=summary}; the detail keeps the full
 * {@code ServiceRequestResponse}.</p>
 */
@Schema(
    name = "ServiceRequestSummaryResponse",
    description = "Service request as shown in a list",
    example = "{\"id\": 42, \"equipment_id\": 10, \"site_id\": 1, \"technician_id\": 7, \"type\": \"REPAIR\", "
        + "\"priority\": \"HIGH\", \"status\": \"COMPLETED\", \"created_at\": \"2026-10-23T13:00:00Z\", "
        + "\"assigned_at\": \"2026-10-23T13:10:00Z\", \"started_at\": \"2026-10-23T14:05:00Z\", "
        + "\"completed_at\": \"2026-10-23T16:30:00Z\", \"canceled_at\": null, \"equipment_name\": \"Cold Room #01\", "
        + "\"site_name\": \"Central Warehouse\", \"technician_name\": \"Ana Torres\", \"has_intervention\": true, "
        + "\"has_review\": false, \"source_alert_id\": 301}")
public record ServiceRequestSummaryResource(
    @Schema(description = "Service request unique identifier", example = "42")
    Long id,

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

    @Schema(description = "Current lifecycle status", example = "COMPLETED")
    ServiceStatusResource status,

    @Schema(description = "When the request was created, ISO-8601 UTC", example = "2026-10-23T13:00:00Z")
    String createdAt,

    @Schema(description = "When the current technician was assigned", example = "2026-10-23T13:10:00Z", nullable = true)
    String assignedAt,

    @Schema(description = "When the work started", example = "2026-10-23T14:05:00Z", nullable = true)
    String startedAt,

    @Schema(description = "When the work finished", example = "2026-10-23T16:30:00Z", nullable = true)
    String completedAt,

    @Schema(description = "When the owner canceled", example = "2026-10-23T17:00:00Z", nullable = true)
    String canceledAt,

    @Schema(description = "Name of the unit; null when the unit is no longer known", example = "Cold Room #01",
        nullable = true)
    String equipmentName,

    @Schema(description = "Name of the site; null when the site is no longer known", example = "Central Warehouse",
        nullable = true)
    String siteName,

    @Schema(description = "Full name of the assigned technician; null while none is assigned", example = "Ana Torres",
        nullable = true)
    String technicianName,

    @Schema(description = "Whether an intervention was registered, which is the case once COMPLETED", example = "true")
    boolean hasIntervention,

    @Schema(description = "Whether the owner reviewed the request", example = "false")
    boolean hasReview,

    @Schema(description = "The alert this corrective request was opened for; null when none", example = "301",
        nullable = true)
    Long sourceAlertId
) {
}
