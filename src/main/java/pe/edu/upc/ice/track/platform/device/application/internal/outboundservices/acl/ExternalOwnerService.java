package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound ACL through which Device Management resolves the owner represented by an
 * authenticated username.
 *
 * <p>This is deliberately the only class in the {@code device} bounded context that knows IAM
 * and Profiles exist. It uses only their published ACL facades and exchanges primitive values, so
 * Device Management never depends on another context's domain model, repositories or services.</p>
 */
@Service
public class ExternalOwnerService {

  private static final long NO_USER = 0L;
  private static final long NO_OWNER = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  public ExternalOwnerService(
      IamContextFacade iamContextFacade,
      ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * Resolves the owner profile represented by a platform username.
   *
   * @param username authenticated principal name
   * @return the owner profile identifier, or empty when the account is unknown or is not an owner
   */
  public Optional<Long> fetchOwnerIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    if (userId == null || userId == NO_USER) {
      return Optional.empty();
    }
    var ownerId = profilesContextFacade.fetchOwnerIdByUserId(userId);
    return ownerId == null || ownerId == NO_OWNER ? Optional.empty() : Optional.of(ownerId);
  }
}
