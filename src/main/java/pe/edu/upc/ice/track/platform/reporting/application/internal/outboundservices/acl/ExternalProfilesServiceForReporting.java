package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which reporting resolves <em>who the caller
 * is</em>: the platform account a report belongs to, and the owner profile whose sites and units
 * the report may cover.
 *
 * <p>Chains the IAM and Profiles facades, both made of primitives and strings, so no IAM or
 * Profiles type is reachable from here.</p>
 */
@Service
public class ExternalProfilesServiceForReporting {

  /** Sentinel the IAM and profiles facades return when nothing matches. */
  private static final long NOT_FOUND = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  public ExternalProfilesServiceForReporting(
      IamContextFacade iamContextFacade, ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * @param username username of the authenticated account
   * @return the platform account identifier, or empty when no account holds that username
   */
  public Optional<Long> fetchUserIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(iamContextFacade.fetchUserIdByUsername(username))
        .filter(ExternalProfilesServiceForReporting::isFound);
  }

  /**
   * @param userId identifier of the platform account
   * @return the owner profile identifier, or empty when the account holds no owner profile
   */
  public Optional<Long> fetchOwnerIdByUserId(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(profilesContextFacade.fetchOwnerIdByUserId(userId))
        .filter(ExternalProfilesServiceForReporting::isFound);
  }

  private static boolean isFound(Long identifier) {
    return identifier != null && identifier != NOT_FOUND;
  }
}
