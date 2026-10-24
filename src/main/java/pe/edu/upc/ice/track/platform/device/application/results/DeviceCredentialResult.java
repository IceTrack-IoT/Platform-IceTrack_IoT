package pe.edu.upc.ice.track.platform.device.application.results;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

/**
 * Result returned only while issuing or rotating a credential.
 *
 * <p>The plaintext key is intentionally carried outside the aggregate and must be shown once to
 * the caller; it is never written to persistence or returned by ordinary device queries.</p>
 */
public record DeviceCredentialResult(Device device, String apiKey) {
}
