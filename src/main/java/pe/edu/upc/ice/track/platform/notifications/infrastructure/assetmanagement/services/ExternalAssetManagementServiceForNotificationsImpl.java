package pe.edu.upc.ice.track.platform.notifications.infrastructure.assetmanagement.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.assetmanagement.ExternalAssetManagementServiceForNotifications;

import java.util.Optional;

/**
 * Adapter fulfilling {@link ExternalAssetManagementServiceForNotifications} on top of the
 * real {@link AssetContextFacade}, the same ACL already consumed by Monitoring and
 * Alerting Management's own outbound adapter -- reused here rather than duplicated.
 */
@Service
public class ExternalAssetManagementServiceForNotificationsImpl
    implements ExternalAssetManagementServiceForNotifications {

  private final AssetContextFacade assetContextFacade;

  public ExternalAssetManagementServiceForNotificationsImpl(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  @Override
  public Optional<Double> fetchEquipmentOwnerId(Long equipmentId) {
    return assetContextFacade.getEquipmentThreshold(equipmentId);
  }
}
