package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the {@code service.request} bounded context checks that a unit
 * exists and belongs to an owner before a request is opened against it, or its intervention history
 * is read.
 *
 * <p>This is the <em>outbound half</em> of the Anti-Corruption Layer between {@code service.request}
 * and {@code assets}, and the only class in {@code service.request} that knows an {@code assets}
 * context exists. It talks to it exclusively through {@link AssetContextFacade}, the inbound facade,
 * whose whole signature is primitives: no assets aggregate, repository or JPA entity is reachable
 * from here.</p>
 *
 * <p>Tenancy is decided by {@code assets} itself: a unit belongs to whoever owns the site it is
 * installed at, and the facade applies that rule rather than this class re-implementing it.</p>
 */
@Service
public class ExternalEquipmentService {

  private final AssetContextFacade assetContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param assetContextFacade the inbound ACL facade of the assets bounded context
   */
  public ExternalEquipmentService(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * Resolves a unit that belongs to an owner.
   *
   * @param ownerId     identifier of the owner profile expected to own the unit
   * @param equipmentId identifier of the unit
   * @return the unit with its site, or empty when it does not exist or belongs to another owner -
   *         the two cases are deliberately indistinguishable
   */
  public Optional<EquipmentRef> fetchOwnedEquipment(Long ownerId, Long equipmentId) {
    if (ownerId == null || equipmentId == null) {
      return Optional.empty();
    }
    if (!assetContextFacade.existsEquipmentForOwner(equipmentId, ownerId)) {
      return Optional.empty();
    }
    return assetContextFacade.getSiteIdByEquipment(equipmentId)
        .map(siteId -> new EquipmentRef(equipmentId, ownerId, siteId));
  }
}
