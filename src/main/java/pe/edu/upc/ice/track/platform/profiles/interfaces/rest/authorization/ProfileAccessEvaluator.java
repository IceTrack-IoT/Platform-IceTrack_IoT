package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalServiceRequestService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.OwnerQueryService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Ownership checks used by the method security expressions of the profiles REST controllers.
 *
 * <p>Referenced from {@code @PreAuthorize} as {@code @profileAccessEvaluator}. The caller is
 * identified by the name of the Spring Security {@link Authentication} - the username - which is
 * resolved to its platform account identifier through the {@link ExternalIamService} ACL, so no
 * IAM security type is ever inspected here. A profile that does not exist is reported as not
 * owned, so the caller receives a 403 rather than learning which identifiers exist.</p>
 *
 * <p>It also decides how much of a profile a caller may <em>read</em> ({@link ProfileView}), so
 * that personal data never leaks through the directory endpoints: a caller sees all of
 * its own profile; an owner and a technician working together on an active service request see
 * each other's contact channels; an owner sees only the professional data of other technicians;
 * and anything else is answered as not found.</p>
 */
@Component("profileAccessEvaluator")
public class ProfileAccessEvaluator {

  private final OwnerQueryService ownerQueryService;
  private final TechnicianQueryService technicianQueryService;
  private final ExternalIamService externalIamService;
  private final ExternalServiceRequestService externalServiceRequestService;

  public ProfileAccessEvaluator(
      OwnerQueryService ownerQueryService,
      TechnicianQueryService technicianQueryService,
      ExternalIamService externalIamService,
      ExternalServiceRequestService externalServiceRequestService) {
    this.ownerQueryService = ownerQueryService;
    this.technicianQueryService = technicianQueryService;
    this.externalIamService = externalIamService;
    this.externalServiceRequestService = externalServiceRequestService;
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

  /**
   * Checks whether the authenticated caller is the platform account itself, as required to read
   * or change account-scoped data such as the dashboard configuration.
   *
   * @param userId         identifier of the account being accessed
   * @param authentication the current authentication
   * @return {@code true} when the identifier is the caller's own account
   */
  public boolean isAccountSelf(Long userId, Authentication authentication) {
    if (userId == null) return false;
    return resolveUserId(authentication)
        .map(userId::equals)
        .orElse(false);
  }

  /**
   * Decides how much of an owner profile the caller may read.
   *
   * @param owner          the owner profile being read
   * @param authentication the current authentication
   * @return {@link ProfileView#FULL} for the owner itself, {@link ProfileView#CONTACT} for a
   *         technician actively assigned to one of the owner's requests, {@link ProfileView#NONE}
   *         otherwise
   */
  public ProfileView ownerProfileViewFor(OwnerProfile owner, Authentication authentication) {
    var userId = resolveUserId(authentication);
    if (userId.isEmpty()) {
      return ProfileView.NONE;
    }
    if (userId.get().equals(owner.getUserId().userId())) {
      return ProfileView.FULL;
    }
    return technicianQueryService.handle(new GetTechnicianByUserIdQuery(new UserId(userId.get())))
        .filter(technician -> externalServiceRequestService.hasActiveEngagementBetween(
            owner.getUserProfileId(), technician.getUserProfileId()))
        .map(technician -> ProfileView.CONTACT)
        .orElse(ProfileView.NONE);
  }

  /**
   * Decides how much of a technician profile the caller may read.
   *
   * @param technician     the technician profile being read
   * @param authentication the current authentication
   * @return {@link ProfileView#FULL} for the technician itself, {@link ProfileView#CONTACT} for an
   *         owner with a request actively assigned to the technician, {@link ProfileView#PUBLIC}
   *         for any other owner, {@link ProfileView#NONE} otherwise (another technician)
   */
  public ProfileView technicianProfileViewFor(TechnicianProfile technician, Authentication authentication) {
    var userId = resolveUserId(authentication);
    if (userId.isEmpty()) {
      return ProfileView.NONE;
    }
    if (userId.get().equals(technician.getUserId().userId())) {
      return ProfileView.FULL;
    }
    return ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(userId.get())))
        .map(owner -> externalServiceRequestService.hasActiveEngagementBetween(
            owner.getUserProfileId(), technician.getUserProfileId()) ? ProfileView.CONTACT : ProfileView.PUBLIC)
        .orElse(ProfileView.NONE);
  }

  /**
   * Resolves the owner profile the caller acts as.
   *
   * @param authentication the current authentication
   * @return the caller's owner profile, or empty when it holds none
   */
  public Optional<OwnerProfile> resolveOwnerProfile(Authentication authentication) {
    return resolveUserId(authentication)
        .flatMap(userId -> ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(userId))));
  }

  /**
   * Resolves the platform account the caller signed in with.
   *
   * @param authentication the current authentication
   * @return the account identifier, or empty when the caller is not authenticated or has no account
   */
  public Optional<Long> resolveAccountId(Authentication authentication) {
    return resolveUserId(authentication);
  }

  private Optional<Long> resolveUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    return externalIamService.fetchUserIdByUsername(authentication.getName());
  }
}
