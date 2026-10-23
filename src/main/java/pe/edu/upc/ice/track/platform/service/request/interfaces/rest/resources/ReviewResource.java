package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing the review of a service request.
 */
@Schema(
    name = "ReviewResponse",
    description = "Owner's review of the technician who completed a service request",
    example = "{\"id\": 5, \"service_request_id\": 42, \"owner_id\": 3, \"technician_id\": 7, \"communication\": 5, "
        + "\"efficacy\": 4, \"performance\": 5, \"average_rating\": 4.67, \"comment\": \"Quick and clear\", "
        + "\"created_at\": \"2026-10-23T17:00:00Z\", \"editable_until\": \"2026-10-25T17:00:00Z\"}")
public record ReviewResource(
    @Schema(description = "Review unique identifier", example = "5")
    Long id,

    @Schema(description = "Reviewed service request", example = "42")
    Long serviceRequestId,

    @Schema(description = "Owner who wrote the review", example = "3")
    Long ownerId,

    @Schema(description = "Technician the review is about", example = "7")
    Long technicianId,

    @Schema(description = "Communication score, from 1 to 5", example = "5")
    Integer communication,

    @Schema(description = "Efficacy score, from 1 to 5", example = "4")
    Integer efficacy,

    @Schema(description = "Performance score, from 1 to 5", example = "5")
    Integer performance,

    @Schema(description = "Mean of the three scores, rounded to two decimals", example = "4.67")
    Double averageRating,

    @Schema(description = "Free-text comment", example = "Quick and clear", nullable = true)
    String comment,

    @Schema(description = "When the review was submitted, ISO-8601 UTC", example = "2026-10-23T17:00:00Z")
    String createdAt,

    @Schema(description = "Until when the review may be edited, ISO-8601 UTC", example = "2026-10-25T17:00:00Z")
    String editableUntil
) {
}
