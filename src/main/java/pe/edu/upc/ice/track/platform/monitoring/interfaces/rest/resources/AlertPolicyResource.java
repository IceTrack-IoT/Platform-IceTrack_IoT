package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for an alert policy.
 */
@Schema(name = "AlertPolicyResponse", description = "The alert policy applicable to an equipment")
public record AlertPolicyResource(
    @Schema(description = "Policy unique identifier", example = "11")
    Long id,

    @Schema(description = "Identifier of the equipment this policy applies to; "
        + "null for the platform-wide default policy", example = "1")
    Long equipmentId,

    @Schema(description = "Minutes an out-of-range reading must persist before an alert opens", example = "2")
    Integer sustainedExcursionMinutes,

    @Schema(description = "Margin, in Celsius, a reading must re-enter range by before the alert closes",
        example = "0.5")
    Double hysteresisMarginCelsius,

    @Schema(description = "Number of missed synchronisation windows before a device is considered offline",
        example = "3")
    Integer missedSyncWindowsForOffline,

    @Schema(description = "Whether this policy is currently active", example = "true")
    boolean active
) {
}
