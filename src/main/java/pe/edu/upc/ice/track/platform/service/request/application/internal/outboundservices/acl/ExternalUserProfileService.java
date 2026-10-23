package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the {@code service.request} bounded context resolves <em>who the
 * caller is</em> before acting on its behalf, and checks that a technician may be assigned work.
 *
 * <p>This is the outbound half of two Anti-Corruption Layers chained, and the only class in
 * {@code service.request} that knows IAM or Profiles exist. A request only carries a bearer token
 * whose principal is a username, while every rule of this context is stated in terms of owner and
 * technician profiles. Resolving that username to an account through {@link IamContextFacade} and
 * then that account to the profile it holds through {@link ProfilesContextFacade} is the same chain
 * {@code assets} follows in its {@code ExternalOwnerService}.</p>
 *
 * <p>Both facades are made of primitives and strings, so no IAM or Profiles aggregate, entity,
 * repository or security type is reachable from here. They answer {@code 0L} for "not found", which
 * is turned into {@link Optional#empty()} here so that sentinel never leaks into this context. An
 * account that holds no profile of the asked role is a legitimate state, and it is the caller's job
 * to decide which status code reports it.</p>
 */
@Service
public class ExternalUserProfileService {

  /**
   * Sentinel the IAM facade returns when no account matches a username.
   */
  private static final long NO_USER = 0L;

  /**
   * Sentinel the profiles facade returns when the account holds no profile of the asked role.
   */
  private static final long NO_PROFILE = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param iamContextFacade      the inbound ACL facade of the IAM bounded context
   * @param profilesContextFacade the inbound ACL facade of the profiles bounded context
   */
  public ExternalUserProfileService(
      IamContextFacade iamContextFacade,
      ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * Resolves the owner profile a username acts as.
   *
   * @param username username of the platform account, may be {@code null} or blank
   * @return the owner profile identifier, or empty when no account holds that username or the
   *         account holds no owner profile
   */
  public Optional<Long> fetchOwnerIdByUsername(String username) {
    return fetchUserIdByUsername(username)
        .flatMap(userId -> toProfileId(profilesContextFacade.fetchOwnerIdByUserId(userId)));
  }

  /**
   * Resolves the technician profile a username acts as.
   *
   * @param username username of the platform account, may be {@code null} or blank
   * @return the technician profile identifier, or empty when no account holds that username or the
   *         account holds no technician profile
   */
  public Optional<Long> fetchTechnicianIdByUsername(String username) {
    return fetchUserIdByUsername(username)
        .flatMap(userId -> toProfileId(profilesContextFacade.fetchTechnicianIdByUserId(userId)));
  }

  /**
   * Tells whether a technician profile is ready to be assigned a service request.
   *
   * <p>What makes a technician ready is a rule of {@code profiles}, so it is asked there rather than
   * re-implemented here.</p>
   *
   * @param technicianProfileId identifier of the technician profile
   * @return {@code true} when the profile exists, is a technician's and is ready for assignment
   */
  public boolean isTechnicianEligibleForAssignment(Long technicianProfileId) {
    return technicianProfileId != null
        && profilesContextFacade.isTechnicianEligibleForAssignment(technicianProfileId);
  }

  private Optional<Long> fetchUserIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    return userId == null || userId == NO_USER ? Optional.empty() : Optional.of(userId);
  }

  private static Optional<Long> toProfileId(Long profileId) {
    return profileId == null || profileId == NO_PROFILE ? Optional.empty() : Optional.of(profileId);
  }
}
