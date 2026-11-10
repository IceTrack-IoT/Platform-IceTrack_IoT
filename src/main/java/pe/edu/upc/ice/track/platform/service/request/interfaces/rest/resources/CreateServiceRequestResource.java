package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Resource received to create a service request.
 *
 * <p>Carries the unit to service but neither an owner nor a site: the owner is resolved from the
 * authenticated caller and must own the unit, and the site is the one the unit is installed at. A
 * corrective request may name the alert it answers, which links the two.</p>
 */
@Schema(
    name = "CreateServiceRequestRequest",
    description = "Service request creation request",
    example = "{\"equipment_id\": 10, \"type\": \"REPAIR\", \"priority\": \"HIGH\", \"description\": \"The display case does not hold 2 to 8 °C\", \"source_alert_id\": 301}")
public record CreateServiceRequestResource(
    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Identifier of the unit to service", example = "10")
    Long equipmentId,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Kind of work", example = "REPAIR")
    ServiceTypeResource type,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Urgency", example = "HIGH")
    ServicePriorityResource priority,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 1000, message = "{validation.size.max}")
    @Schema(description = "What the owner needs", example = "The display case does not hold 2 to 8 °C", maxLength = 1000)
    String description,

    @Positive(message = "{validation.positive}")
    @Schema(description = "The alert this corrective request is opened for; it must be one of the caller's "
        + "alerts, raised on the same unit, in any status", example = "301", nullable = true)
    Long sourceAlertId
) {
}
