package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.assetmanagement;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which the notifications application layer
 * resolves the recipient of an equipment-related notification.
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
}
