package pe.edu.upc.ice.track.platform.device.infrastructure.assetsmanagement.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;

/** ACL adapter from Device Management to the published Assets Management facade. */
@Service
public class DeviceExternalAssetManagementServiceImpl implements ExternalAssetManagementService {

  private final AssetContextFacade assetContextFacade;

  public DeviceExternalAssetManagementServiceImpl(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  @Override
  public boolean existsEquipmentForOwner(Long equipmentId, Long ownerId) {
    return equipmentId != null && ownerId != null
        && assetContextFacade.existsEquipmentForOwner(equipmentId, ownerId);
  }
}
