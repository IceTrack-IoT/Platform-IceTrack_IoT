package pe.edu.upc.ice.track.platform.device.application.queryservices;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceAuthenticationResult;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesByEquipmentQuery;

import java.util.List;
import java.util.Optional;

/** Application contract for reading and authenticating managed devices. */
public interface DeviceQueryService {
  Optional<Device> handle(GetDeviceByIdQuery query);

  List<Device> handle(GetDevicesByEquipmentQuery query);

  /**
   * Authenticates an edge request by its API key alone.
   *
   * @param apiKey the value of the {@code X-API-Key} header
   * @return the server-side device and equipment identifiers, or empty when the key is malformed,
   *         unknown, revoked or belongs to an unpaired board
   */
  Optional<DeviceAuthenticationResult> authenticateByApiKey(String apiKey);
}
