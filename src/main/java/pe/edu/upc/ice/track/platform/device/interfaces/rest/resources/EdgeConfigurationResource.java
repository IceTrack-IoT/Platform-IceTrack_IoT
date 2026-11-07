package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource carrying the configuration an edge gateway enforces for its unit.
 */
@Schema(
    name = "EdgeConfigurationResponse",
    description = "Configuration the edge gateway enforces locally: threshold, alert policy and technician grants",
    example = "{\"config_version\": 7, \"min_celsius\": 2.0, \"max_celsius\": 8.0, "
        + "\"sustained_excursion_minutes\": 2, \"hysteresis_margin_celsius\": 1.0, "
        + "\"missed_sync_windows_for_offline\": 3, \"grants\": [{\"sha256\": "
        + "\"5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8\", \"technician_id\": 7, "
        + "\"service_request_id\": 42, \"expires_at\": \"2026-11-08T14:00:00Z\"}]}")
public record EdgeConfigurationResource(
    @Schema(description = "Version of this configuration; also returned as the ETag header", example = "7")
    Integer configVersion,

    @Schema(description = "Lowest acceptable temperature in Celsius", example = "2.0")
    Double minCelsius,

    @Schema(description = "Highest acceptable temperature in Celsius", example = "8.0")
    Double maxCelsius,

    @Schema(description = "Minutes an excursion must last before the gateway raises it", example = "2")
    Integer sustainedExcursionMinutes,

    @Schema(description = "Margin, in Celsius, the temperature must recover by relative to the breached limit "
        + "before the gateway closes an excursion", example = "1.0")
    Double hysteresisMarginCelsius,

    @Schema(description = "Missed synchronization windows before a device counts as offline", example = "3")
    Integer missedSyncWindowsForOffline,

    @Schema(description = "Technician grants the gateway must accept; empty when none is active")
    List<EdgeGrantResource> grants
) {

  /**
   * A technician grant the gateway must accept.
   *
   * @param sha256           hex SHA-256 digest of the grant token presented by the mobile application
   * @param technicianId     the technician the grant was issued to
   * @param serviceRequestId the request it was issued for
   * @param expiresAt        when it stops being valid, ISO-8601 UTC
   */
  @Schema(name = "EdgeGrantEntry", description = "Technician grant the gateway must accept")
  public record EdgeGrantResource(
      @Schema(description = "Hex SHA-256 digest of the grant token",
          example = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8")
      String sha256,

      @Schema(description = "Technician the grant was issued to", example = "7")
      Long technicianId,

      @Schema(description = "Service request the grant was issued for", example = "42")
      Long serviceRequestId,

      @Schema(description = "When the grant stops being valid", example = "2026-11-08T14:00:00Z")
      String expiresAt
  ) {
  }
}
