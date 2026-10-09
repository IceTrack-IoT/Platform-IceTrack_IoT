package pe.edu.upc.ice.track.platform.assets.interfaces.rest.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.ExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.SiteQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;

import java.util.Optional;

/**
 * Ownership checks used by the method security expressions of the assets REST controllers.
 *
 * <p>Referenced from {@code @PreAuthorize} as {@code @assetAccessEvaluator}, mirroring
 * {@code profiles.interfaces.rest.authorization.ProfileAccessEvaluator}. The caller is identified
 * by the name of the Spring Security {@link Authentication} - the username - which is resolved to
 * its owner profile identifier through the {@link ExternalOwnerService} ACL, so no IAM security
 * type is ever inspected here. A site or equipment that does not exist, or that belongs to another
 * owner, is reported as not owned, so the caller receives a 403 rather than learning which
 * identifiers exist.</p>
 */
@Component("assetAccessEvaluator")
public class AssetAccessEvaluator {

  private final SiteQueryService siteQueryService;
  private final EquipmentQueryService equipmentQueryService;
  private final ExternalOwnerService externalOwnerService;

  public AssetAccessEvaluator(
      SiteQueryService siteQueryService,
      EquipmentQueryService equipmentQueryService,
      ExternalOwnerService externalOwnerService) {
    this.siteQueryService = siteQueryService;
    this.equipmentQueryService = equipmentQueryService;
    this.externalOwnerService = externalOwnerService;
  }

  /**
   * Checks whether the authenticated caller owns the site being accessed.
   *
   * @param siteId identifier of the site being accessed
   * @param authentication the current authentication
   * @return {@code true} when the site exists and belongs to the caller's owner profile
   */
  public boolean isSiteOwnedBy(Long siteId, Authentication authentication) {
    if (siteId == null) {
      return false;
    }
    return resolveOwnerId(authentication)
        .flatMap(ownerId -> siteQueryService.handle(new GetSiteByIdQuery(siteId, ownerId))
            .map(site -> ownerId.equals(site.getOwnerId())))
        .orElse(false);
  }

  /**
   * Checks whether the authenticated caller owns the equipment being accessed.
   *
   * @param equipmentId identifier of the equipment being accessed
   * @param authentication the current authentication
   * @return {@code true} when the equipment exists and belongs to the caller's owner profile
   */
  public boolean isEquipmentOwnedBy(Long equipmentId, Authentication authentication) {
    if (equipmentId == null) {
      return false;
    }
    return resolveOwnerId(authentication)
        .flatMap(ownerId -> equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId, ownerId))
            .map(equipment -> true))
        .orElse(false);
  }

  private Optional<Long> resolveOwnerId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    return externalOwnerService.fetchOwnerIdByUsername(authentication.getName());
  }
}
