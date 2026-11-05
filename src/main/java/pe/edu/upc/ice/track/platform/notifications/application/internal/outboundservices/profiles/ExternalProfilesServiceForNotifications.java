package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.profiles;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which notifications resolves <em>who the caller
 * is</em>: the recipient identifier that notifications are addressed to (the caller's owner
 * profile).
 *
 * <p>Chains the IAM and Profiles facades: username -> account -> owner profile.</p>
 */
@Service
public class ExternalProfilesServiceForNotifications {

  private static final long NOT_FOUND = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  public ExternalProfilesServiceForNotifications(
      IamContextFacade iamContextFacade, ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * @param username username of the authenticated account
   * @return the owner profile identifier, or empty when the account does not exist or holds no owner profile
   */
  public Optional<Long> fetchRecipientUserIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    if (userId == null || userId == NOT_FOUND) {
      return Optional.empty();
    }
    var ownerId = profilesContextFacade.fetchOwnerIdByUserId(userId);
    return ownerId == null || ownerId == NOT_FOUND ? Optional.empty() : Optional.of(ownerId);
  }
}
