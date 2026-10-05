package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Resource received to replace the descriptive data and maintenance interval of a unit.
 *
 * <p>Deliberately narrower than the registration payload: the uid is the unit's identity as printed
 * on the device and must stay stable across a rename, and the threshold has its own endpoint so
 * that changing it always publishes its own domain event.</p>
 */
@Schema(
    name = "UpdateEquipmentRequest",
    description = "Equipment update request",
    example = "{\"name\": \"Congelador Principal\", \"equipment_type\": \"FREEZER\", \"reminder_interval_days\": 120}")
public record UpdateEquipmentResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Name assigned to the unit", example = "Congelador Principal", maxLength = 30)
    String name,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Kind of refrigeration unit", example = "FREEZER")
    EquipmentTypeResource equipmentType,

    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Preventive maintenance interval in days", example = "120")
    Integer reminderIntervalDays
) {
}