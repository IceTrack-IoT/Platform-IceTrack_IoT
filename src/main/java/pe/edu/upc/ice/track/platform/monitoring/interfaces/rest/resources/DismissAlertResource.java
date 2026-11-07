package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to dismiss an alert as a false alarm.
 *
 * <p>The dismissing user is the authenticated caller; it is never part of the payload.</p>
 */
@Schema(
    name = "DismissAlertRequest",
    description = "Justification for dismissing an alert that does not represent an actual fault",
    example = "{\"reason\": \"Planned defrost cycle\", \"evidence\": \"Defrost schedule DF-2026-11, 08:00-08:30\"}"
)
public record DismissAlertResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 500, message = "{validation.size.max}")
    @Schema(
        description = "Why the alert does not represent an actual fault",
        example = "Planned defrost cycle",
        maxLength = 500,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String reason,

    @Size(max = 1000, message = "{validation.size.max}")
    @Schema(
        description = "Optional supporting evidence, such as a note or a link to a maintenance record",
        example = "Defrost schedule DF-2026-11, 08:00-08:30",
        maxLength = 1000,
        nullable = true
    )
    String evidence
) {
}
