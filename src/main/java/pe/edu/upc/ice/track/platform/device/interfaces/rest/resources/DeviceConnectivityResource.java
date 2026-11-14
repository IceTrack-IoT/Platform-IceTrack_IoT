package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Connectivity of a board, evaluated by the server when it is read.
 *
 * <p>Every property is always present; the nullable ones are sent as {@code null}.</p>
 *
 * @param state              the evaluated state
 * @param silentAfterSeconds the offline window of the paired unit's policy, in seconds; {@code null}
 *                           when unpaired
 * @param silentSince        when the board turned silent; {@code null} unless the state is SILENT
 * @param evaluatedAt        the server instant of the evaluation
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
    name = "DeviceConnectivity",
    description = "Connectivity of a board evaluated by the server at read time, from the server arrival time "
        + "of its latest batch and the offline window of its unit's alert policy",
    example = "{\"state\": \"REPORTING\", \"silent_after_seconds\": 180, \"silent_since\": null, "
        + "\"evaluated_at\": \"2026-11-09T15:02:40Z\"}")
public record DeviceConnectivityResource(
    @Schema(description = "Evaluated connectivity state", example = "REPORTING")
    DeviceConnectivityStateResource state,

    @Schema(description = "Offline window of the paired unit's alert policy, in seconds; null when unpaired",
        example = "180", nullable = true)
    Integer silentAfterSeconds,

    @Schema(description = "When the board turned silent (last_seen_at plus the window); null unless SILENT",
        example = "2026-11-09T14:54:12Z", nullable = true)
    Instant silentSince,

    @Schema(description = "Server time of the evaluation", example = "2026-11-09T15:02:40Z")
    Instant evaluatedAt) {
}
