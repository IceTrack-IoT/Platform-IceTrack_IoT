package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.profiles;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which notifications resolves <em>who the caller
 * is</em>: the recipient identifier that notifications are addressed to, which is the caller's owner
 * profile or, for a technician, its technician profile.
 *
 * <p>Chains the IAM and Profiles facades: username -> account -> profile. Owner and technician
 * profiles share one identifier sequence, so a recipient identifier never names two people.</p>
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
   * @return the owner or technician profile identifier, or empty when the account does not exist or
   *         holds no profile
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
    if (ownerId != null && ownerId != NOT_FOUND) {
      return Optional.of(ownerId);
    }
    var technicianId = profilesContextFacade.fetchTechnicianIdByUserId(userId);
    return technicianId == null || technicianId == NOT_FOUND ? Optional.empty() : Optional.of(technicianId);
  }
}
