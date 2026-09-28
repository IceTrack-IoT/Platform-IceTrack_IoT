package pe.edu.upc.ice.track.platform.iam.interfaces.acl;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByUsernameQuery;

/**
 * ACL facade that exposes IAM bounded context capabilities to other contexts.
 *
 * <p>Provides a simplified integration surface for querying identity data without leaking IAM
 * internal model details.</p>
 *
 * <p>Account creation is deliberately absent from this facade: an account can only be registered
 * through the authentication endpoints, together with its role and the onboarding form
 * its profile requires. Federated authentication is absent too: a Google id_token can only be
 * verified through the authentication endpoints, never from another bounded context.</p>
 */
@Service
public class IamContextFacade {
  private final UserQueryService userQueryService;

  public IamContextFacade(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  /**
   * Fetches the identifier for a username.
   *
   * @param username username to search
   * @return user identifier, or {@code 0L} when user is not found
   */
  public Long fetchUserIdByUsername(String username) {
    var getUserByUsernameQuery = new GetUserByUsernameQuery(username);
    var result = userQueryService.handle(getUserByUsernameQuery);
    if (result.isEmpty()) return 0L;
    return result.get().getId();
  }

  /**
   * Fetches the username for a user identifier.
   *
   * @param userId user identifier
   * @return username, or an empty string when user is not found
   */
  public String fetchUsernameByUserId(Long userId) {
    var getUserByIdQuery = new GetUserByIdQuery(userId);
    var result = userQueryService.handle(getUserByIdQuery);
    if (result.isEmpty()) return Strings.EMPTY;
    return result.get().getUsername();
  }

}
