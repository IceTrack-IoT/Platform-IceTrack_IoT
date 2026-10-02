package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.SessionTokens;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;

/**
 * Assembler that translates IAM authentication results into {@link AuthenticatedUserResource}.
 */
public class AuthenticatedUserResourceFromEntityAssembler {
  /**
   * Creates a resource from the authenticated {@link User} aggregate and its issued session tokens.
   *
   * @param user authenticated user aggregate
   * @param sessionTokens generated access and refresh tokens
   * @return resource used by the authentication endpoint response
   */
  public static AuthenticatedUserResource toResourceFromEntity(User user, SessionTokens sessionTokens) {
    return new AuthenticatedUserResource(
        user.getId(),
        user.getUsername(),
        user.getRoleName(),
        sessionTokens.accessToken(),
        sessionTokens.refreshToken());
  }
}
