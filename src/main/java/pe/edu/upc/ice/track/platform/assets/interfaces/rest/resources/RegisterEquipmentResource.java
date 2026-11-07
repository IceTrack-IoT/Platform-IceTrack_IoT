package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Resource received to register a refrigeration unit at a site.
 *
 * <p>Carries the site the unit belongs to, but not an owner: the owner is resolved from the
 * authenticated caller and must match the owner of that site, which is what stops a unit from being
 * catalogued somewhere it does not belong.</p>
 *
 * <p>Each of {@code minCelsius} and {@code maxCelsius} must lie within 0 to 30 °C, checked here.
 * Their ordering is validated as a pair by the domain: a relational rule needs both values, and
 * Bean Validation can only express it through a class-level constraint that would have to be
 * written by hand.</p>
 */
@Schema(
    name = "RegisterEquipmentRequest",
    description = "Equipment registration request",
    example = "{\"site_id\": 1, \"uid\": \"ESP32-0001\", \"name\": \"Refrigerador 1\", \"equipment_type\": \"REFRIGERATOR\", \"min_celsius\": 2.0, \"max_celsius\": 8.0, \"reminder_interval_days\": 90}")
public record RegisterEquipmentResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Identifier of the site the unit is installed at", example = "1")
    Long siteId,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "The unit's own identifier, unique across the platform", example = "ESP32-0001", maxLength = 30)
    String uid,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Name assigned to the unit", example = "Refrigerador 1", maxLength = 30)
    String name,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Kind of refrigeration unit", example = "REFRIGERATOR")
    EquipmentTypeResource equipmentType,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.threshold.range}")
    @DecimalMax(value = "30.0", message = "{validation.threshold.range}")
    @Schema(description = "Lowest acceptable temperature in Celsius, within 0 to 30 and strictly below max_celsius",
        example = "2.0", minimum = "0", maximum = "30")
    Double minCelsius,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.threshold.range}")
    @DecimalMax(value = "30.0", message = "{validation.threshold.range}")
    @Schema(description = "Highest acceptable temperature in Celsius, within 0 to 30 and strictly above min_celsius",
        example = "8.0", minimum = "0", maximum = "30")
    Double maxCelsius,

    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Preventive maintenance interval in days", example = "90")
    Integer reminderIntervalDays
) {
}