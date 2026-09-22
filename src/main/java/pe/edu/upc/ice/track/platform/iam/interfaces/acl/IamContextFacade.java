package pe.edu.upc.ice.track.platform.iam.interfaces.acl;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByUsernameQuery;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * ACL facade that exposes IAM bounded context capabilities to other contexts.
 *
 * <p>Provides a simplified integration surface for creating users and querying identity data
 * without leaking IAM internal model details.</p>
 *
 * <p>Federated authentication is deliberately absent from this facade: a Google id_token can only
 * be exchanged through the authentication endpoint, never from another bounded context.</p>
 */
@Service
public class IamContextFacade {
  private final UserCommandService userCommandService;
  private final UserQueryService userQueryService;

  public IamContextFacade(UserCommandService userCommandService, UserQueryService userQueryService) {
    this.userCommandService = userCommandService;
    this.userQueryService = userQueryService;
  }

  /**
   * Creates a new user assigning the default role.
   *
   * <p>The email is required: registering an account also provisions its profile in the
   * {@code profiles} bounded context, and a profile is identified by its email address.</p>
   *
   * @param username username to register
   * @param password raw password
   * @param email email address of the account holder
   * @return created user identifier, or {@code 0L} when creation fails
   */
  public Long createUser(String username, String password, String email) {
    var signUpCommand = new SignUpByLocalCommand(username, password, email, List.of(Role.getDefaultRole()));
    var result = userCommandService.handle(signUpCommand);
    if (result instanceof Result.Success(var user)) {
      return user.getId();
    }
    return 0L;
  }

  /**
   * Creates a new user with explicit role names.
   *
   * @param username username to register
   * @param password raw password
   * @param email email address of the account holder
   * @param roleNames role names to assign; unknown names are ignored
   * @return created user identifier, or {@code 0L} when creation fails
   */
  public Long createUser(String username, String password, String email, List<String> roleNames) {
    var roles = roleNames != null ? roleNames.stream().map(Role::toRoleFromName).toList() : new ArrayList<Role>();
    var signUpCommand = new SignUpByLocalCommand(username, password, email, roles);
    var result = userCommandService.handle(signUpCommand);
    if (result instanceof Result.Success(var user)) {
      return user.getId();
    }
    return 0L;
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
