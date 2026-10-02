package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.assetmanagement;

import java.util.Optional;

/**
 * Outbound port (of the Anti-Corruption Layer between {@code monitoring} and
 * {@code assetmanagement}) through which the monitoring application layer retrieves the
 * temperature threshold of an equipment before evaluating an incoming reading.
 *
 * <p>The monitoring application layer depends on this port and on nothing else: it never sees
 * an Asset Management aggregate, repository or command. The adapter implementing it, in
 * {@code monitoring.infrastructure.assetmanagement.services}, is the only class in
 * {@code monitoring} allowed to know that an {@code assetmanagement} context exists.</p>
 */
public interface ExternalAssetManagementService {

  /**
   * @param equipmentId identifier of the equipment to evaluate; required
   * @return the threshold as {@code [minCelsius, maxCelsius]}, or empty when the equipment
   *         does not exist
   */
  Optional<double[]> fetchTemperatureThreshold(Long equipmentId);
}
