package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.assets;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.List;

/**
 * Outbound service (of the Anti-Corruption Layer between {@code reporting} and {@code assets})
 * through which the reporting strategies discover which units belong to a site.
 *
 * <p>This is the only class in {@code reporting} that knows an {@code assets} context exists. It
 * talks to it exclusively through {@link AssetContextFacade}: no Assets aggregate, repository or
 * command is reachable from here. The facade resolves a site's units whatever their owner: it
 * performs no ownership check.</p>
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
}
