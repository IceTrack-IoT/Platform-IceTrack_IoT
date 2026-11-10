package pe.edu.upc.ice.track.platform.device.domain.model.queries;

/**
 * Query for the configuration an edge gateway must enforce.
 *
 * @param equipmentId the unit the authenticated device is paired with, taken from its API key;
 *                    required
 */
public record GetEdgeConfigurationQuery(Long equipmentId) {

  public GetEdgeConfigurationQuery {
    if (equipmentId == null) {
      throw new IllegalArgumentException("equipmentId must not be null");
    }
  }
}
