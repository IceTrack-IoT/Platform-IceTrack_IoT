package pe.edu.upc.ice.track.platform.device.application.queryservices;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceAuthenticationResult;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetails;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetailsPage;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesQuery;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;

/** Application contract for reading and authenticating managed devices. */
public interface DeviceQueryService {

  /**
   * Fetches one of the caller's boards.
   *
   * @param query the query naming the board and the caller
   * @return the board with the unit it is linked to, or empty when it does not exist or is not
   *         linked to a unit of the caller
   */
  Optional<DeviceDetails> handle(GetDeviceByIdQuery query);

  /**
   * Lists the boards linked to the units of the caller, narrowed by the optional filters of the
   * query.
   *
   * @param query the query naming the caller, the filters and the requested slice
   * @return the matching boards, each with the unit it is linked to, ordered by board uid; or a
   *         {@code SITE_NOT_FOUND} or {@code EQUIPMENT_NOT_FOUND} failure when the query names a
   *         site or a unit that does not exist or is not the caller's
   */
  Result<DeviceDetailsPage, ApplicationError> handle(GetDevicesQuery query);

  /**
   * Describes a board with the unit it is linked to.
   *
   * <p>For a board the caller was already allowed to act on, such as the one a command just
   * changed: no ownership is checked here.</p>
   *
   * @param device the board
   * @return the board with the name and the site of its unit, both {@code null} when it is linked to
   *         none
   */
  DeviceDetails describe(Device device);

  /**
   * Authenticates an edge request by its API key alone.
   *
   * @param apiKey the value of the {@code X-API-Key} header
   * @return the server-side device and equipment identifiers, or empty when the key is malformed,
   *         unknown, revoked or belongs to an unpaired board
   */
  Optional<DeviceAuthenticationResult> authenticateByApiKey(String apiKey);
}
