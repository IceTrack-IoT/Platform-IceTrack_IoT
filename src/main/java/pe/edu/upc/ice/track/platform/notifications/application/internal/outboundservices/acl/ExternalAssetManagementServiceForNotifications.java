package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentSummary;

import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which the notifications application layer
 * resolves the recipient of an equipment-related notification, and the name and band of the unit
 * that the notification's template names.
 *
 * <p>This is the only class in {@code notifications} that knows an {@code assets} context exists.
 * It talks to it exclusively through {@link AssetContextFacade}: no Asset Management aggregate,
 * repository or command is reachable from here.</p>
 */
@Service
public class ExternalAssetManagementServiceForNotifications {

  private final AssetContextFacade assetContextFacade;

  public ExternalAssetManagementServiceForNotifications(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * @param equipmentId identifier of the equipment to resolve; required
   * @return the identifier of the equipment's owner, or empty when the equipment or its site does
   *         not exist
   */
  public Optional<Long> fetchEquipmentOwnerId(Long equipmentId) {
    return assetContextFacade.getEquipmentOwnerId(equipmentId);
  }

  /**
   * @param equipmentId identifier of the equipment, may be {@code null}
   * @return the name of the equipment, or empty when it is unknown
   */
  public Optional<String> fetchEquipmentName(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return assetContextFacade.fetchEquipmentSummary(equipmentId).map(EquipmentSummary::name);
  }

  /**
   * @param equipmentId identifier of the equipment; required
   * @return the acceptable band of the equipment as {@code [min, max]} in Celsius, or empty when the
   *         equipment does not exist
   */
  public Optional<double[]> fetchThresholdRange(Long equipmentId) {
    return assetContextFacade.getEquipmentThresholdRange(equipmentId);
  }
}
