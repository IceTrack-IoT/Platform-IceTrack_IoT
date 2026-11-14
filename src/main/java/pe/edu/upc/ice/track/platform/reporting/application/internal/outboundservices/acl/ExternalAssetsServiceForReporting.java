package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.List;
import java.util.Optional;

/**
 * Outbound service (of the Anti-Corruption Layer between {@code reporting} and {@code assets})
 * through which the reporting strategies discover which units belong to a site.
 *
 * <p>This is the only class in {@code reporting} that knows an {@code assets} context exists. It
 * talks to it exclusively through {@link AssetContextFacade}: no Assets aggregate, repository or
 * command is reachable from here. Listing a site's units performs no ownership check by itself, so
 * the command service checks the requested scope with {@link #isSiteOwnedBy} and
 * {@link #isEquipmentOwnedBy} before any strategy runs.</p>
 */
@Service
public class ExternalAssetsServiceForReporting {

  private final AssetContextFacade assetContextFacade;

  public ExternalAssetsServiceForReporting(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * @param siteId identifier of the site; required
   * @return the identifiers of every unit installed at that site, empty when the site has none
   *         or does not exist
   */
  public List<Long> fetchEquipmentIdsBySite(Long siteId) {
    return assetContextFacade.fetchEquipmentIdsBySite(siteId);
  }

  /**
   * @param siteId  identifier of the site
   * @param ownerId identifier of the owner profile
   * @return {@code true} only when the site exists and belongs to that owner
   */
  public boolean isSiteOwnedBy(Long siteId, Long ownerId) {
    return assetContextFacade.existsSiteForOwner(siteId, ownerId);
  }

  /**
   * @param equipmentId identifier of the unit
   * @param ownerId     identifier of the owner profile
   * @return {@code true} only when the unit exists and its site belongs to that owner
   */
  public boolean isEquipmentOwnedBy(Long equipmentId, Long ownerId) {
    return assetContextFacade.existsEquipmentForOwner(equipmentId, ownerId);
  }

  /**
   * @param equipmentId identifier of the unit
   * @return the acceptable temperature band of the unit as {@code [minCelsius, maxCelsius]}, or empty
   *         when the unit does not exist
   */
  public Optional<double[]> fetchTemperatureThresholdRange(Long equipmentId) {
    return assetContextFacade.getEquipmentThresholdRange(equipmentId);
  }

  /**
   * @param equipmentId identifier of the unit
   * @return the name of the unit and of its site as {@code [equipmentName, siteName]}, either of which may
   *         be {@code null}; empty when the unit does not exist
   */
  public Optional<String[]> fetchEquipmentAndSiteNames(Long equipmentId) {
    return assetContextFacade.fetchEquipmentSummary(equipmentId)
        .map(summary -> new String[] {summary.name(), summary.siteName()});
  }
}
