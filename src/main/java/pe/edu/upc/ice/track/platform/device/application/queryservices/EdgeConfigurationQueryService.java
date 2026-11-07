package pe.edu.upc.ice.track.platform.device.application.queryservices;

import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetEdgeConfigurationQuery;

import java.util.Optional;

/**
 * Application service serving the configuration edge gateways poll.
 */
public interface EdgeConfigurationQueryService {

  /**
   * Composes the configuration of a unit's gateway.
   *
   * @param query the unit the device is paired with
   * @return the configuration, or empty when the unit no longer exists
   */
  Optional<EdgeConfiguration> handle(GetEdgeConfigurationQuery query);
}
