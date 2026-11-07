package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to make sure the platform-wide default alert policy exists.
 *
 * <p>Issued once at startup. Idempotent: when a default policy is already stored, it is left as
 * is. The default carries {@code AlertPolicy}'s platform values (2 sustained minutes, a 1.0 °C
 * hysteresis margin matching the DHT11 resolution, and 3 missed sync windows).</p>
 */
public record SeedDefaultAlertPolicyCommand() {
}
