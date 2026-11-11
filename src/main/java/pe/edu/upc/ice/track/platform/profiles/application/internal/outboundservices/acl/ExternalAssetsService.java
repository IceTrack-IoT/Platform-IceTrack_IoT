package pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

/**
 * Outbound service through which the {@code profiles} bounded context reads how many sites and units
 * an owner has, for the session bootstrap.
 *
 * <p>The only class in {@code profiles} that knows an {@code assets} context exists. It talks to it
 * exclusively through {@link AssetContextFacade}.</p>
 */
@Service
public class ExternalAssetsService {

  private final AssetContextFacade assetContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param assetContextFacade the inbound ACL facade of the Assets bounded context
   */
  public ExternalAssetsService(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * @param ownerProfileId identifier of the owner profile
   * @return how many sites the owner has, {@code 0} when none
   */
  public int countSites(Long ownerProfileId) {
    return assetContextFacade.countSitesByOwner(ownerProfileId);
  }

  /**
   * @param ownerProfileId identifier of the owner profile
   * @return how many units the owner has across its sites, {@code 0} when none
   */
  public long countEquipment(Long ownerProfileId) {
    return assetContextFacade.countEquipmentByOwner(ownerProfileId);
  }
}
