package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.assetmanagement;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.Optional;

/**
 * Outbound service (the outbound half of the Anti-Corruption Layer between {@code monitoring} and
 * {@code assets}) through which the monitoring application layer retrieves the temperature
 * threshold of an equipment before evaluating an incoming reading.
 *
 * <p>This is the only class in {@code monitoring} that knows an {@code assets} context exists. It
 * talks to it exclusively through {@link AssetContextFacade}, whose whole signature is
 * primitives: no assets aggregate, repository or command is reachable from here.</p>
 *
 * <p>The threshold returned is the one actually configured on the unit. There is deliberately no
 * fallback band: when the unit is unknown this service answers empty, and
 * {@code SensorReadingCommandServiceImpl} logs a warning and skips the evaluation rather than
 * judging the reading against a band that does not belong to it.</p>
 */
@Service
public class ExternalAssetManagementService {

  private final AssetContextFacade assetContextFacade;

  public ExternalAssetManagementService(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * @param equipmentId identifier of the equipment to evaluate; required
   * @return the threshold as {@code [minCelsius, maxCelsius]}, or empty when the equipment
   *         does not exist
   */
  public Optional<double[]> fetchTemperatureThreshold(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return assetContextFacade.getEquipmentThresholdRange(equipmentId);
  }
}
