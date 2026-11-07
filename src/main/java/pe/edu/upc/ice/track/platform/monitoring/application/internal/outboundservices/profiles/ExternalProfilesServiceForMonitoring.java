package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.profiles;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which monitoring resolves <em>who the caller
 * is</em>: the owner profile or the technician profile an authenticated username acts as.
 *
 * <p>Chains the IAM and Profiles facades: username -> account -> profile. Both hops are read-only
 * lookups against published facades made of primitives and strings, so no IAM or Profiles type is
 * reachable from here.</p>
 */
@Service
public class ExternalProfilesServiceForMonitoring {

  /** Sentinel the IAM and profiles facades return when nothing matches. */
  private static final long NOT_FOUND = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  public ExternalProfilesServiceForMonitoring(
      IamContextFacade iamContextFacade, ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * @param username username of the authenticated account
   * @return the owner profile identifier, or empty when the account does not exist or holds no
   *         owner profile
   */
  public Optional<Long> fetchOwnerIdByUsername(String username) {
    return fetchUserIdByUsername(username)
        .map(profilesContextFacade::fetchOwnerIdByUserId)
        .filter(ExternalProfilesServiceForMonitoring::isFound);
  }

  /**
   * @param username username of the authenticated account
   * @return the technician profile identifier, or empty when the account does not exist or holds no
   *         technician profile
   */
  public Optional<Long> fetchTechnicianIdByUsername(String username) {
    return fetchUserIdByUsername(username)
        .map(profilesContextFacade::fetchTechnicianIdByUserId)
        .filter(ExternalProfilesServiceForMonitoring::isFound);
  }

  /**
   * @param username username of the authenticated account
   * @return the IAM identifier of the account, or empty when it does not exist
   */
  public Optional<Long> fetchUserIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(iamContextFacade.fetchUserIdByUsername(username))
        .filter(ExternalProfilesServiceForMonitoring::isFound);
  }

  private static boolean isFound(Long identifier) {
    return identifier != null && identifier != NOT_FOUND;
  }
}
