package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials;

/**
 * One freshly issued device credential, split into its delivery and persistence representations.
 *
 * @param plainText the API key handed to the owner exactly once
 * @param digest    hex SHA-256 digest of {@code plainText}, the only form ever stored
 */
public record IssuedDeviceCredential(String plainText, String digest) {
}
