package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource carrying a freshly issued edge grant, plaintext token included.
 *
 * <p>Returned only by the response that issued the grant: accepting a request, or regenerating its
 * grant. The token is never shown again; the mobile application keeps it in secure storage and
 * presents it to the on-site gateway.</p>
 */
@Schema(
    name = "IssuedEdgeGrantResponse",
    description = "Edge grant issued to the assigned technician; the token is shown only once",
    example = "{\"token\": \"it_grt_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08\", "
        + "\"service_request_id\": 42, \"technician_id\": 7, \"equipment_id\": 10, "
        + "\"issued_at\": \"2026-11-07T14:00:00Z\", \"expires_at\": \"2026-11-08T14:00:00Z\"}")
public record IssuedEdgeGrantResource(
    @Schema(description = "Plaintext grant token: it_grt_ followed by 64 hexadecimal characters. Store it "
        + "securely; it cannot be retrieved again, only regenerated",
        example = "it_grt_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
    String token,

    @Schema(description = "Service request the grant was issued for", example = "42")
    Long serviceRequestId,

    @Schema(description = "Technician the grant was issued to", example = "7")
    Long technicianId,

    @Schema(description = "Unit whose gateway the grant opens", example = "10")
    Long equipmentId,

    @Schema(description = "When the grant was issued", example = "2026-11-07T14:00:00Z")
    String issuedAt,

    @Schema(description = "When the grant stops being valid", example = "2026-11-08T14:00:00Z")
    String expiresAt
) {
}
