package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.OwnerQueryService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;

import java.util.Optional;

/**
 * Ownership checks used by the method security expressions of the profiles REST controllers.
 *
 * <p>Referenced from {@code @PreAuthorize} as {@code @profileAccessEvaluator}. The caller is
 * identified by the name of the Spring Security {@link Authentication} - the username - which is
 * resolved to its platform account identifier through the {@link ExternalIamService} ACL, so no
 * IAM security type is ever inspected here. A profile that does not exist is reported as not
 * owned, so the caller receives a 403 rather than learning which identifiers exist.</p>
 */
@Component("profileAccessEvaluator")
public class ProfileAccessEvaluator {

  private final OwnerQueryService ownerQueryService;
  private final TechnicianQueryService technicianQueryService;
  private final ExternalIamService externalIamService;

  public ProfileAccessEvaluator(
      OwnerQueryService ownerQueryService,
      TechnicianQueryService technicianQueryService,
      ExternalIamService externalIamService) {
    this.ownerQueryService = ownerQueryService;
    this.technicianQueryService = technicianQueryService;
    this.externalIamService = externalIamService;
  }

  /**
   * Checks whether the authenticated caller is the account the owner belongs to.
   *
   * @param ownerId        identifier of the owner being accessed
   * @param authentication the current authentication
   * @return {@code true} when the owner exists and belongs to the caller's account
   */
  public boolean isOwnerSelf(Long ownerId, Authentication authentication) {
    if (ownerId == null) return false;
    return resolveUserId(authentication)
        .flatMap(userId -> ownerQueryService.handle(new GetOwnerByIdQuery(ownerId))
            .map(owner -> userId.equals(owner.getUserId().userId())))
        .orElse(false);
  }

  /**
   * Checks whether the authenticated caller is the account the technician belongs to.
   *
   * @param technicianId   identifier of the technician being accessed
   * @param authentication the current authentication
   * @return {@code true} when the technician exists and belongs to the caller's account
   */
  public boolean isTechnicianSelf(Long technicianId, Authentication authentication) {
    if (technicianId == null) return false;
    return resolveUserId(authentication)
        .flatMap(userId -> technicianQueryService.handle(new GetTechnicianByIdQuery(technicianId))
            .map(technician -> userId.equals(technician.getUserId().userId())))
        .orElse(false);
  }

  private Optional<Long> resolveUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    return externalIamService.fetchUserIdByUsername(authentication.getName());
  }
}
