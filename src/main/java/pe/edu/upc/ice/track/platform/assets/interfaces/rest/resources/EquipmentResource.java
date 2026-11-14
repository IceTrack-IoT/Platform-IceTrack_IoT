package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Outbound REST resource representing a refrigeration unit.
 *
 * <p>When listing to show - name, type, uid and status - plus
 * the detail a unit opens with. The telemetry columns {@code online}, {@code last_reading_at} and
 * {@code last_known_temperature} are exposed but not writable: they are written by the Monitoring
 * and Alerting context, never through these endpoints.</p>
 *
 * <p>{@code site_name} is read-only too: it is the name of the site at the time of the read, there
 * so a client can show where a unit is installed without also listing the sites. A site is renamed
 * through the site endpoints.</p>
 *
 * <p>{@code connectivity} is evaluated by the server on every read, from the server arrival time of
 * the unit's latest batch. It supersedes {@code online}, which is kept for compatibility.</p>
 */
@Schema(
    name = "EquipmentResponse",
    description = "Equipment information response",
    example = "{\"id\": 10, \"site_id\": 1, \"site_name\": \"Sede Central\", \"uid\": \"ESP32-0001\", \"name\": \"Refrigerador 1\", \"equipment_type\": \"REFRIGERATOR\", \"status\": \"ACTIVE\", \"online\": true, \"min_celsius\": 2.0, \"max_celsius\": 8.0, \"reminder_interval_days\": 90, \"last_reading_at\": \"2026-02-01T10:15:30Z\", \"last_known_temperature\": 4.5, \"config_version\": 3, \"version\": 2}")
public record EquipmentResource(
    @Schema(description = "Equipment unique identifier", example = "10")
    Long id,

    @Schema(description = "Identifier of the site the unit is installed at", example = "1")
    Long siteId,

    @Schema(
        description = "Name of the site the unit is installed at, at the time of the read; read-only",
        example = "Sede Central",
        accessMode = Schema.AccessMode.READ_ONLY)
    String siteName,

    @Schema(description = "The unit's own identifier, unique across the platform", example = "ESP32-0001")
    String uid,

    @Schema(description = "Name assigned to the unit", example = "Refrigerador 1")
    String name,

    @Schema(description = "Kind of refrigeration unit", example = "REFRIGERATOR")
    EquipmentTypeResource equipmentType,

    @Schema(description = "Current lifecycle status", example = "ACTIVE")
    StatusEquipmentResource status,

    @Schema(description = "Deprecated in favor of connectivity.state. Whether the unit is reporting: true once a "
        + "reading batch is stored; false before the first one, after offline detection and while DEACTIVATED",
        example = "true", deprecated = true)
    boolean online,

    @Schema(description = "Lowest acceptable temperature in Celsius", example = "2.0")
    Double minCelsius,

    @Schema(description = "Highest acceptable temperature in Celsius", example = "8.0")
    Double maxCelsius,

    @Schema(description = "Preventive maintenance interval in days", example = "90")
    Integer reminderIntervalDays,

    @Schema(
        description = "When the unit's latest reading batch was taken (its recorded_at); a batch delivered late never moves it back; null when the unit has not reported yet",
        example = "2026-02-01T15:15:30Z",
        nullable = true)
    Instant lastReadingAt,

    @Schema(
        description = "Average temperature of the unit's latest reading batch, in Celsius; null when the unit has not reported yet",
        example = "4.5",
        nullable = true)
    Double lastKnownTemperature,

    @Schema(
        description = "Version of the configuration the unit's edge gateway enforces; incremented whenever "
            + "the threshold, the alert policy or the technician grants of the unit change",
        example = "3")
    Integer configVersion,

    @Schema(description = "Connectivity evaluated by the server at read time; read-only",
        accessMode = Schema.AccessMode.READ_ONLY)
    EquipmentConnectivityResource connectivity,

    @Schema(description = "Concurrency version; grows on every change of name, type, reminder interval, threshold "
        + "or status, not on telemetry. Send it back in If-Match to update the unit", example = "2",
        accessMode = Schema.AccessMode.READ_ONLY)
    Long version
) {
}