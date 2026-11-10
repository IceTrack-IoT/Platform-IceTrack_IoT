package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Outbound service through which the {@code service.request} bounded context checks that a unit
 * exists and belongs to an owner before a request is opened against it, or its intervention history
 * is read, and resolves the unit and site names shown with a request.
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

  /**
   * Tells whether a unit exists and belongs to an owner.
   *
   * @param ownerId     identifier of the owner profile expected to own the unit
   * @param equipmentId identifier of the unit
   * @return {@code true} only when the unit exists and belongs to that owner
   */
  public boolean isEquipmentOwnedBy(Long ownerId, Long equipmentId) {
    return ownerId != null && equipmentId != null && assetContextFacade.existsEquipmentForOwner(equipmentId, ownerId);
  }

  /**
   * Returns the name of every unit of an owner, by unit identifier.
   *
   * @param ownerId identifier of the owner profile
   * @return the unit names, empty when the owner is {@code null} or has no unit
   */
  public Map<Long, String> fetchEquipmentNamesByOwner(Long ownerId) {
    if (ownerId == null) {
      return Map.of();
    }
    var names = new HashMap<Long, String>();
    for (var equipment : assetContextFacade.fetchEquipmentSummariesByOwner(ownerId, null)) {
      if (equipment.equipmentId() != null && equipment.name() != null) {
        names.put(equipment.equipmentId(), equipment.name());
      }
    }
    return Map.copyOf(names);
  }

  /**
   * Returns the name of every site of an owner, by site identifier.
   *
   * @param ownerId identifier of the owner profile
   * @return the site names, empty when the owner is {@code null} or has no site
   */
  public Map<Long, String> fetchSiteNamesByOwner(Long ownerId) {
    return ownerId == null ? Map.of() : assetContextFacade.fetchSiteNamesByOwner(ownerId);
  }
}
