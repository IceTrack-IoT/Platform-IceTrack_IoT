package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for an ice track owner profile.
 *
 * <p>Carries only the shared profile attributes and the owner's own RUC: no technician attribute
 * ever appears in an owner payload.</p>
 */
@Schema(
    name = "OwnerProfileResponse",
    description = "Owner profile information response",
    example = "{\"id\": 1, \"userId\": 42, \"fullName\": \"John Doe\", \"email\": \"john.doe@example.com\", \"phone\": \"+51 987654321\", \"address\": \"Av. Primavera 123, Lima, 15023, Peru\", \"ruc\": 20123456789}"
)
public record OwnerProfileResource(
    @Schema(description = "Owner profile unique identifier", example = "1")
    Long id,

    @Schema(description = "Identifier of the platform account the owner belongs to", example = "42")
    Long userId,

    @Schema(description = "Owner full name", example = "John Doe")
    String fullName,

    @Schema(description = "Owner email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "Owner phone number", example = "+51 987654321")
    String phone,

    @Schema(description = "Owner complete street address", example = "Av. Primavera 123, Lima, 15023, Peru")
    String address,

    @Schema(description = "11 digit taxpayer registration number (RUC)", example = "20123456789")
    Long ruc
) {
}
