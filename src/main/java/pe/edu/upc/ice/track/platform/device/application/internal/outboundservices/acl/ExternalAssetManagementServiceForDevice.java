package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.List;
import java.util.Optional;

/**
 * Outbound service through which Device Management checks an equipment before pairing a board,
 * reads the threshold and configuration version its edge gateway enforces, and resolves the units
 * of an owner to scope and describe its device inventory.
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
   * @return the owner of the unit, resolved through its site, or empty when the unit or its site
   *         does not exist
   */
  public Optional<Long> fetchOwnerId(Long equipmentId) {
    return equipmentId == null ? Optional.empty() : assetContextFacade.getEquipmentOwnerId(equipmentId);
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

  /**
   * @param siteId  identifier of the site
   * @param ownerId identifier of the owner expected to own it
   * @return {@code true} only when the site exists and belongs to that owner
   */
  public boolean existsSiteForOwner(Long siteId, Long ownerId) {
    return siteId != null && ownerId != null && assetContextFacade.existsSiteForOwner(siteId, ownerId);
  }

  /**
   * Lists the units of an owner, which is what scopes its device inventory: a board belongs to
   * whoever owns the unit it is linked to.
   *
   * @param ownerId identifier of the owner
   * @param siteId  restrict to the units of a single site, may be {@code null}
   * @return the units a board of that owner can be linked to, empty when the owner is {@code null}
   */
  public List<PairedEquipment> fetchEquipmentOwnedBy(Long ownerId, Long siteId) {
    if (ownerId == null) {
      return List.of();
    }
    return assetContextFacade.fetchEquipmentSummariesByOwner(ownerId, siteId).stream()
        .map(unit -> new PairedEquipment(unit.equipmentId(), unit.name(), unit.siteId()))
        .toList();
  }

  /**
   * @param equipmentId identifier of the equipment
   * @return the unit a board is linked to, or empty when the identifier is {@code null} or the unit
   *         does not exist
   */
  public Optional<PairedEquipment> fetchPairedEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return assetContextFacade.fetchEquipmentSummary(equipmentId)
        .map(unit -> new PairedEquipment(unit.equipmentId(), unit.name(), unit.siteId()));
  }

  /**
   * The unit a board is linked to, in this context's own terms: what a device listing shows of it.
   *
   * @param equipmentId identifier of the unit
   * @param name        name assigned to the unit
   * @param siteId      identifier of the site the unit is installed at
   */
  public record PairedEquipment(Long equipmentId, String name, Long siteId) {
  }
}
