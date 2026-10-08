package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.UserResource;

/**
 * Assembler that converts IAM {@link User} aggregates into REST {@link UserResource} objects.
 */
public class UserResourceFromEntityAssembler {
  /**
   * Converts a user aggregate to its REST representation.
   *
   * @param user user aggregate
   * @return user resource
   */
  public static UserResource toResourceFromEntity(User user) {
    return new UserResource(user.getId(), user.getUsername(), user.getRoleName(), user.getProviderName());
  }
}
