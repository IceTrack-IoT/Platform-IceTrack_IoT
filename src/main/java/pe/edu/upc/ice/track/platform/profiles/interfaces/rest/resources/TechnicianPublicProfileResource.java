package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What an owner may see of any technician when choosing whom to assign work to: professional data
 * and projected operational statistics, and no personal data - no email, phone number or home
 * address.
 */
@Schema(
    name = "TechnicianPublicProfileResponse",
    description = "Public technician profile with operational statistics",
    example = "{\"id\": 7, \"full_name\": \"Jane Doe\", \"speciality\": \"Refrigeration\", "
        + "\"certification_number\": \"CERT-2024-001\", \"completed_services\": 12, \"average_rating\": 4.6, "
        + "\"rating_available\": true}")
public record TechnicianPublicProfileResource(
    @Schema(description = "Technician profile unique identifier", example = "7")
    Long id,

    @Schema(description = "Technician full name", example = "Jane Doe")
    String fullName,

    @Schema(description = "Technician speciality", example = "Refrigeration")
    String speciality,

    @Schema(description = "Technician certification number", example = "CERT-2024-001")
    String certificationNumber,

    @Schema(description = "Service requests the technician completed", example = "12")
    Integer completedServices,

    @Schema(description = "Average of the technician's review scores, between 1 and 5; null until the first review",
        example = "4.6", nullable = true)
    Double averageRating,

    @Schema(description = "Whether average_rating is available, i.e. the technician has been reviewed", example = "true")
    Boolean ratingAvailable
) {
}
