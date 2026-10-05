package pe.edu.upc.ice.track.platform.notifications.infrastructure.assetmanagement.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetManagementContextFacade;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.assetmanagement.ExternalAssetManagementServiceForNotifications;

/**
 * Adapter fulfilling {@link ExternalAssetManagementServiceForNotifications} on top of the
 * real {@link AssetManagementContextFacade}, the same ACL already consumed by Monitoring and
 * Alerting Management's own outbound adapter -- reused here rather than duplicated.
 */
@Service
public class ExternalAssetManagementServiceForNotificationsImpl
    implements ExternalAssetManagementServiceForNotifications {

  private final AssetManagementContextFacade assetManagementContextFacade;

  public ExternalAssetManagementServiceForNotificationsImpl(AssetManagementContextFacade assetManagementContextFacade) {
    this.assetManagementContextFacade = assetManagementContextFacade;
  }

  @Override
  public Long fetchEquipmentOwnerId(Long equipmentId) {
    return assetManagementContextFacade.fetchEquipmentOwnerId(equipmentId);
  }
}
