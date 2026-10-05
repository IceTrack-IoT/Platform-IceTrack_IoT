package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Resource for creating or updating an alert policy.
 */
@Schema(
    name = "UpdateAlertPolicyRequest",
    description = "Request payload to create or update an alert policy, global or per-equipment",
    example = "{\"equipmentId\": 1, \"sustainedExcursionMinutes\": 2, "
        + "\"hysteresisMarginCelsius\": 0.5, \"missedSyncWindowsForOffline\": 3}"
)
public record UpdateAlertPolicyResource(
    @Schema(description = "Identifier of the equipment this policy applies to; "
        + "omit to target the platform-wide default policy", example = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Long equipmentId,

    @NotNull(message = "{validation.not-blank}")
    @Positive
    @Schema(description = "Minutes an out-of-range reading must persist before an alert opens", example = "2")
    Integer sustainedExcursionMinutes,

    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Margin, in Celsius, a reading must re-enter range by before the alert closes",
        example = "0.5")
    Double hysteresisMarginCelsius,

    @NotNull(message = "{validation.not-blank}")
    @Positive
    @Schema(description = "Number of missed synchronisation windows before a device is considered offline",
        example = "3")
    Integer missedSyncWindowsForOffline
) {
}
