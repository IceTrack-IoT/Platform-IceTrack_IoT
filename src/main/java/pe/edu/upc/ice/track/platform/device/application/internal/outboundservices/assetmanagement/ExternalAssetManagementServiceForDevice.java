package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.Optional;

/**
 * Outbound service through which Device Management checks an equipment before pairing a board,
 * and reads the threshold and configuration version its edge gateway enforces.
 *
 * <p>This is the only class in this context allowed to know about Assets Management; command and
 * query services depend on it instead of on an asset aggregate or repository, and it talks to
 * {@code assets} exclusively through {@link AssetContextFacade}.</p>
 */
@Service
public class ExternalAssetManagementServiceForDevice {

  private final AssetContextFacade assetContextFacade;

  public ExternalAssetManagementServiceForDevice(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * @param equipmentId identifier of the equipment
   * @param ownerId     identifier of the owner expected to own it
   * @return {@code true} only when the unit exists and belongs to that owner
   */
  public boolean existsEquipmentForOwner(Long equipmentId, Long ownerId) {
    return equipmentId != null && ownerId != null
        && assetContextFacade.existsEquipmentForOwner(equipmentId, ownerId);
  }

  /**
   * @param equipmentId identifier of the equipment
   * @return the version of the configuration its edge gateway must enforce, or empty when the unit
   *         does not exist
   */
  public Optional<Integer> fetchConfigVersion(Long equipmentId) {
    return assetContextFacade.getEquipmentConfigVersion(equipmentId);
  }

  /**
   * @param equipmentId identifier of the equipment
   * @return the temperature band as {@code [minCelsius, maxCelsius]}, or empty when the unit does
   *         not exist
   */
  public Optional<double[]> fetchThresholdRange(Long equipmentId) {
    return assetContextFacade.getEquipmentThresholdRange(equipmentId);
  }
}
