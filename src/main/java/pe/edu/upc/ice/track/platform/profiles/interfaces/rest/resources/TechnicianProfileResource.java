package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for an ice track maintenance technician profile.
 *
 * <p>Carries only the shared profile attributes and the technician's own speciality and
 * certification: no owner attribute ever appears in a technician payload.</p>
 */
@Schema(
    name = "TechnicianProfileResponse",
    description = "Technician profile information response",
    example = "{\"id\": 1, \"userId\": 43, \"fullName\": \"Jane Doe\", \"email\": \"jane.doe@example.com\", \"phone\": \"+51 987654321\", \"address\": \"Av. Primavera 123, Lima, 15023, Peru\", \"speciality\": \"Refrigeration\", \"certificationNumber\": \"CERT-2024-001\"}"
)
public record TechnicianProfileResource(
    @Schema(description = "Technician profile unique identifier", example = "1")
    Long id,

    @Schema(description = "Identifier of the platform account the technician belongs to", example = "43")
    Long userId,

    @Schema(description = "Technician full name", example = "Jane Doe")
    String fullName,

    @Schema(description = "Technician email address", example = "jane.doe@example.com")
    String email,

    @Schema(description = "Technician phone number", example = "+51 987654321")
    String phone,

    @Schema(description = "Technician complete street address", example = "Av. Primavera 123, Lima, 15023, Peru")
    String address,

    @Schema(description = "Technician speciality", example = "Refrigeration")
    String speciality,

    @Schema(description = "Technician certification number", example = "CERT-2024-001")
    String certificationNumber
) {
}
