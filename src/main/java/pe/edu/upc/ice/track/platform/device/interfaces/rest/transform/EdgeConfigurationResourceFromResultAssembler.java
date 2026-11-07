package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.EdgeConfigurationResource;

/**
 * Assembler converting an {@link EdgeConfiguration} into an {@link EdgeConfigurationResource}.
 */
public final class EdgeConfigurationResourceFromResultAssembler {

  private EdgeConfigurationResourceFromResultAssembler() {
  }

  public static EdgeConfigurationResource toResourceFromResult(EdgeConfiguration configuration) {
    return new EdgeConfigurationResource(
        configuration.configVersion(),
        configuration.minCelsius(),
        configuration.maxCelsius(),
        configuration.sustainedExcursionMinutes(),
        configuration.hysteresisMarginCelsius(),
        configuration.missedSyncWindowsForOffline(),
        configuration.grants().stream()
            .map(grant -> new EdgeConfigurationResource.EdgeGrantResource(
                grant.tokenSha256(),
                grant.technicianProfileId(),
                grant.serviceRequestId(),
                grant.expiresAt().toString()))
            .toList());
  }
}
