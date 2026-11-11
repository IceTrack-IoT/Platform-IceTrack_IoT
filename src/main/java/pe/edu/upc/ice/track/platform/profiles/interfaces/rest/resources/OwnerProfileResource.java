package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for an ice track owner profile.
 *
 * <p>Carries only the shared profile attributes and the owner's own RUC: no technician attribute
 * ever appears in an owner payload.</p>
 *
 * <p>{@code address} is the composed text; {@code street}, {@code number}, {@code city},
 * {@code postal_code} and {@code country} are the same address in the parts an update takes, so a
 * settings form can be filled in without parsing the text. A caller who may only see the owner's
 * contact channels receives every address field, the RUC and the version as {@code null}.</p>
 */
@Schema(
    name = "OwnerProfileResponse",
    description = "Owner profile information response",
    example = "{\"id\": 1, \"user_id\": 42, \"full_name\": \"John Doe\", \"email\": \"john.doe@example.com\", "
        + "\"phone\": \"+51 987654321\", \"address\": \"Av. Primavera 123, Lima, 15023, Peru\", "
        + "\"street\": \"Av. Primavera\", \"number\": \"123\", \"city\": \"Lima\", \"postal_code\": \"15023\", "
        + "\"country\": \"Peru\", \"ruc\": 20123456789, \"version\": 2}"
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

    @Schema(description = "Owner complete street address, composed from its parts",
        example = "Av. Primavera 123, Lima, 15023, Peru")
    String address,

    @Schema(description = "Street of the address", example = "Av. Primavera")
    String street,

    @Schema(description = "Street number or apartment; null when the address has none", example = "123",
        nullable = true)
    String number,

    @Schema(description = "City of the address", example = "Lima")
    String city,

    @Schema(description = "Postal code of the address", example = "15023")
    String postalCode,

    @Schema(description = "Country of the address", example = "Peru")
    String country,

    @Schema(description = "11 digit taxpayer registration number (RUC)", example = "20123456789")
    Long ruc,

    @Schema(description = "Concurrency version; grows on every change. Send it back in If-Match to update the "
        + "profile", example = "2")
    Long version
) {
}
