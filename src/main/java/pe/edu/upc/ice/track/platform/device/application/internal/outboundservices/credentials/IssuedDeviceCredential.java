package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials;

/** One freshly issued device credential, split into delivery and persistence representations. */
public record IssuedDeviceCredential(String plainText, String hash) {
}
