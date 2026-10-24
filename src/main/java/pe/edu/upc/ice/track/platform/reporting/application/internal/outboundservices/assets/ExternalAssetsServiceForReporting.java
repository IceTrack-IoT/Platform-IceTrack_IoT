package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.assets;

import java.util.List;

/**
 * Outbound port (of the Anti-Corruption Layer between {@code reporting} and {@code assets})
 * through which {@code MaintenanceComplianceStrategy} discovers which units belong to a site.
 *
 * <p>The reporting application layer depends on this port and on nothing else: it never sees an
 * Assets aggregate, repository or command. The adapter implementing it, in
 * {@code reporting.infrastructure.assets.services}, is the only class in {@code reporting}
 * allowed to know that an {@code assets} context exists.</p>
 */
public interface ExternalAssetsServiceForReporting {

  /**
   * @param siteId identifier of the site; required
   * @return the identifiers of every unit installed at that site, empty when the site has none
   *         or does not exist
   */
  List<Long> fetchEquipmentIdsBySite(Long siteId);
}
