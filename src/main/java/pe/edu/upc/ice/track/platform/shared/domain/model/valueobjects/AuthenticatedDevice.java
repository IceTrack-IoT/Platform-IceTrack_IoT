package pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects;

import java.util.Objects;

/**
 * Identity of a paired IoT board, as established by its API key on an edge route.
 *
 * <p>This is the security principal of every edge request. Both identifiers are read from the
 * server-side pairing of the board, never from the request, so a controller that needs to know
 * which device or equipment sent the data takes it from here. It lives in the shared kernel so
 * that Monitoring can read it without importing anything from Device Management.</p>
 *
 * @param deviceId    identifier of the authenticated board
 * @param equipmentId identifier of the refrigeration unit the board is paired with
 */
public record AuthenticatedDevice(Long deviceId, Long equipmentId) {

  public AuthenticatedDevice {
    Objects.requireNonNull(deviceId, "deviceId must not be null");
    Objects.requireNonNull(equipmentId, "equipmentId must not be null");
  }
}
