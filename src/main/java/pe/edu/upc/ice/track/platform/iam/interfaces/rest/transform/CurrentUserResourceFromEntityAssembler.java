package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CurrentUserResource;

import java.util.List;

/**
 * Assembler that translates the authenticated {@link User} aggregate into {@link CurrentUserResource}.
 */
public class CurrentUserResourceFromEntityAssembler {
  /**
   * Creates a resource from the account of the authenticated principal.
   *
   * @param user  the authenticated user aggregate
   * @param email the account holder's email address, read from the profile; may be {@code null}
   * @return the current user resource
   */
  public static CurrentUserResource toResourceFromEntity(User user, String email) {
    return new CurrentUserResource(user.getId(), user.getUsername(), email, user.getRoleName(), user.getProviderName());
  }
}
