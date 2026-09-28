package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for a profile.
 *
 * <p>The role specific fields are only populated for their own role: {@code ruc} for an owner,
 * {@code speciality} and {@code certificationNumber} for a technician.</p>
 */
@Schema(
    name = "ProfileResponse",
    description = "Profile information response",
    example = "{\"id\": 1, \"userId\": 42, \"fullName\": \"John Doe\", \"email\": \"john.doe@example.com\", \"role\": \"OWNER\", \"phoneNumber\": \"+51 987654321\", \"streetAddress\": \"Av. Primavera 123, Lima, 15023, Peru\", \"ruc\": 20123456789, \"speciality\": null, \"certificationNumber\": null}"
)
public record ProfileResource(
    @Schema(description = "Profile unique identifier", example = "1")
    Long id,

    @Schema(description = "User unique identifier associated with the profile", example = "42")
    Long userId,

    @Schema(description = "Profile full name", example = "John Doe")
    String fullName,

    @Schema(description = "Profile email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "Role the profile plays", example = "OWNER", allowableValues = {"OWNER", "TECHNICIAN"})
    String role,

    @Schema(description = "Profile phone number", example = "+51 987654321")
    String phoneNumber,

    @Schema(description = "Complete street address", example = "Av. Primavera 123, Lima, 15023, Peru")
    String streetAddress,

    @Schema(description = "Opaque annotation supplied by the context that requested the profile")
    String auxiliaryData,

    @Schema(description = "Taxpayer registration number; only present for OWNER profiles", example = "20123456789")
    Long ruc,

    @Schema(description = "Technician speciality; only present for TECHNICIAN profiles", example = "Refrigeration")
    String speciality,

    @Schema(description = "Technician certification number; only present for TECHNICIAN profiles", example = "CERT-2024-001")
    String certificationNumber
) {
}
