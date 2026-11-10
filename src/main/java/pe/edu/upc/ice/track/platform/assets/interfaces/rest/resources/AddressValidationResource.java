package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound resource returned when an address has been verified.
 *
 * @param formattedAddress the normalized address
 * @param latitude         latitude of the verified point, in degrees
 * @param longitude        longitude of the verified point, in degrees
 * @param placeId          the provider's place identifier, or {@code null}
 * @param district         the district, or {@code null} when the provider named none
 * @param area             the coverage area: {@code LIMA_METROPOLITANA} or {@code CALLAO}
 * @param validationToken  the signed token to present when saving the site, valid for 15 minutes
 */
@Schema(
    name = "AddressValidationResponse",
    description = "A verified address and the token that proves it when saving a site")
public record AddressValidationResource(
    @Schema(description = "Normalized address", example = "Av. Primavera 123, Santiago de Surco, Lima, Peru")
    String formattedAddress,

    @Schema(description = "Latitude of the verified point", example = "-12.0931")
    Double latitude,

    @Schema(description = "Longitude of the verified point", example = "-77.0465")
    Double longitude,

    @Schema(description = "Provider place identifier", example = "ChIJ8e8c9yLIBZERt0P0wP0xV5A", nullable = true)
    String placeId,

    @Schema(description = "District", example = "Santiago de Surco", nullable = true)
    String district,

    @Schema(description = "Coverage area", example = "LIMA_METROPOLITANA",
        allowableValues = {"LIMA_METROPOLITANA", "CALLAO"})
    String area,

    @Schema(description = "Token to present when saving the site; valid for 15 minutes",
        example = "eyJhbGciOiJIUzI1NiJ9.e30.signature")
    String validationToken
) {
}
