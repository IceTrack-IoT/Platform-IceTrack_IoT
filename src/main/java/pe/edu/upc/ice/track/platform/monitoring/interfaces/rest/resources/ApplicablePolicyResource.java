package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for the alert policy applicable to one unit, as answered by the batch policy read.
 *
 * <p>The unit is named next to the policy because the platform default is bound to no unit: its
 * {@code policy.equipment_id} is null, so the policy alone would not say which unit it answers.</p>
 */
@Schema(
    name = "ApplicablePolicyResponse",
    description = "The alert policy that applies to a unit, and whether it is the unit's own or the platform default",
    example = "{\"equipment_id\": 10, \"source\": \"DEFAULT\", \"policy\": {\"id\": 1, \"equipment_id\": null, "
        + "\"sustained_excursion_minutes\": 2, \"hysteresis_margin_celsius\": 1.0, "
        + "\"missed_sync_windows_for_offline\": 3, \"active\": true}}"
)
public record ApplicablePolicyResource(
    @Schema(description = "Identifier of the unit the policy applies to", example = "10")
    Long equipmentId,

    @Schema(description = "Where the policy comes from: EQUIPMENT for the unit's own policy, DEFAULT for the "
        + "platform-wide default", example = "DEFAULT", allowableValues = {"EQUIPMENT", "DEFAULT"})
    String source,

    @Schema(description = "The applicable policy; its equipment_id is null when source is DEFAULT")
    AlertPolicyResource policy
) {
}
