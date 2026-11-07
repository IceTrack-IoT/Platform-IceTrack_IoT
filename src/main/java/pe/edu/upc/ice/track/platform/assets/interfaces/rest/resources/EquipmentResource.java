package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing a refrigeration unit.
 *
 * <p>When listing to show - name, type, uid and status - plus
 * the detail a unit opens with. The telemetry columns {@code online}, {@code last_reading_at} and
 * {@code last_known_temperature} are exposed but not writable: they are written by the Monitoring
 * and Alerting context, never through these endpoints.</p>
 */
@Schema(
    name = "EquipmentResponse",
    description = "Equipment information response",
    example = "{\"id\": 10, \"site_id\": 1, \"uid\": \"ESP32-0001\", \"name\": \"Refrigerador 1\", \"equipment_type\": \"REFRIGERATOR\", \"status\": \"ACTIVE\", \"online\": true, \"min_celsius\": 2.0, \"max_celsius\": 8.0, \"reminder_interval_days\": 90, \"last_reading_at\": \"2026-02-01T10:15:30\", \"last_known_temperature\": 4.5, \"config_version\": 3}")
public record EquipmentResource(
    @Schema(description = "Equipment unique identifier", example = "10")
    Long id,

    @Schema(description = "Identifier of the site the unit is installed at", example = "1")
    Long siteId,

    @Schema(description = "The unit's own identifier, unique across the platform", example = "ESP32-0001")
    String uid,

    @Schema(description = "Name assigned to the unit", example = "Refrigerador 1")
    String name,

    @Schema(description = "Kind of refrigeration unit", example = "REFRIGERATOR")
    EquipmentTypeResource equipmentType,

    @Schema(description = "Current lifecycle status", example = "ACTIVE")
    StatusEquipmentResource status,

    @Schema(description = "Whether the unit is currently connected", example = "true")
    boolean online,

    @Schema(description = "Lowest acceptable temperature in Celsius", example = "2.0")
    Double minCelsius,

    @Schema(description = "Highest acceptable temperature in Celsius", example = "8.0")
    Double maxCelsius,

    @Schema(description = "Preventive maintenance interval in days", example = "90")
    Integer reminderIntervalDays,

    @Schema(
        description = "When the last reading was received; null when the unit has not reported yet",
        example = "2026-02-01T10:15:30",
        nullable = true)
    String lastReadingAt,

    @Schema(
        description = "Last temperature known for this unit, in Celsius; null when the unit has not reported yet",
        example = "4.5",
        nullable = true)
    Double lastKnownTemperature,

    @Schema(
        description = "Version of the configuration the unit's edge gateway enforces; incremented whenever "
            + "the threshold, the alert policy or the technician grants of the unit change",
        example = "3")
    Integer configVersion
) {
}