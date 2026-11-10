package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One KPI of a completed report.
 *
 * @param key   stable machine key, for translating the label on the client; a KPI computed for a
 *              single unit ends in {@code .unit_<equipmentId>}
 * @param label English label
 * @param value numeric value
 * @param unit  unit of the value; an empty string when the value is a plain count
 */
@Schema(
    name = "KpiResponse",
    description = "One KPI of a completed report",
    example = "{\"key\": \"time_in_range_pct\", \"label\": \"Time in range\", \"value\": 99.2, \"unit\": \"%\"}")
public record KpiResource(
    @Schema(description = "Stable machine key; a per-unit KPI ends in .unit_<equipment_id>", example = "excursion_count")
    String key,

    @Schema(description = "English label", example = "Total excursions")
    String label,

    @Schema(description = "Numeric value", example = "3")
    double value,

    @Schema(description = "Unit of the value: %, h, °C, °C·min or stars; an empty string for a plain count",
        example = "")
    String unit
) {
}
