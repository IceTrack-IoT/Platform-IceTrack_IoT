package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Resource for creating or updating an alert policy.
 */
@Schema(
    name = "UpdateAlertPolicyRequest",
    description = "Request payload to create or update the alert policy of one of the caller's units",
    example = "{\"equipment_id\": 1, \"sustained_excursion_minutes\": 2, "
        + "\"hysteresis_margin_celsius\": 1.0, \"missed_sync_windows_for_offline\": 3}"
)
public record UpdateAlertPolicyResource(
    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Identifier of one of the caller's units this policy applies to. "
        + "The platform-wide default policy is managed by configuration and cannot be changed here.",
        example = "1")
    Long equipmentId,

    @NotNull(message = "{validation.not-blank}")
    @Positive
    @Schema(description = "Minutes an out-of-range reading must persist before an alert opens", example = "2")
    Integer sustainedExcursionMinutes,

    @NotNull(message = "{validation.not-blank}")
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    @Schema(description = "Margin, in Celsius, a reading must re-enter range by before the alert closes; "
        + "at least 1.0, the resolution of the DHT11 sensor", example = "1.0")
    Double hysteresisMarginCelsius,

    @NotNull(message = "{validation.not-blank}")
    @Positive
    @Schema(description = "Number of missed synchronisation windows before a device is considered offline",
        example = "3")
    Integer missedSyncWindowsForOffline
) {
}
