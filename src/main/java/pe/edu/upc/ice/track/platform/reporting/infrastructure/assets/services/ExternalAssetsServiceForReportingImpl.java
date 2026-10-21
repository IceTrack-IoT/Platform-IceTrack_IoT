package pe.edu.upc.ice.track.platform.reporting.infrastructure.assets.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.assets.ExternalAssetsServiceForReporting;

import java.util.List;

/**
 * Adapter for {@link ExternalAssetsServiceForReporting}, wrapping the real {@code assets}
 * context.
 *
 * <p><strong>Facade extension required.</strong> {@code AssetContextFacade} does not yet expose a
 * method to list a site's unit identifiers. Per "Solo genera ese BC," that facade is not modified
 * here; the patch below is documented instead, to be applied by hand to
 * {@code assets/interfaces/acl/AssetContextFacade.java}:</p>
 *
 * <pre>{@code
 * // Add to AssetContextFacade (it already injects an EquipmentRepository):
 * public List<Long> fetchEquipmentIdsBySite(Long siteId) {
 *   return equipmentRepository.findBySiteId(siteId).stream()
 *       .map(Equipment::getId)
 *       .toList();
 * }
 * }</pre>
 *
 * <p>Unlike the Monitoring and Service Requests adapters in this bounded context, this one is not
 * a stub: {@code EquipmentRepository.findBySiteId(Long)} already exists in the real repository and
 * requires no ownership check, so the patch above is a direct, minimal addition. Until it is
 * applied, this adapter throws {@link UnsupportedOperationException}.</p>
 */
@Service
public class ExternalAssetsServiceForReportingImpl implements ExternalAssetsServiceForReporting {

  // private final AssetContextFacade assetContextFacade;
  //
  // public ExternalAssetsServiceForReportingImpl(AssetContextFacade assetContextFacade) {
  //   this.assetContextFacade = assetContextFacade;
  // }

  @Override
  public List<Long> fetchEquipmentIdsBySite(Long siteId) {
    // return assetContextFacade.fetchEquipmentIdsBySite(siteId);
    throw new UnsupportedOperationException(
        "AssetContextFacade.fetchEquipmentIdsBySite(Long) does not exist yet -- "
            + "see this class's Javadoc for the exact patch to apply.");
  }
}
