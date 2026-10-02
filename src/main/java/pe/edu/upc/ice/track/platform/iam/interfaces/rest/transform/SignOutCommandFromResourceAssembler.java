package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignOutCommand;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.RefreshTokenResource;

/**
 * Assembler that converts a {@link RefreshTokenResource} into a {@link SignOutCommand}.
 */
public class SignOutCommandFromResourceAssembler {

  /**
   * Converts the sign-out payload into its command representation.
   *
   * @param resource the {@link RefreshTokenResource} resource to convert; may be {@code null} when
   *                 the request has no body, which the command rejects as a validation error
   * @return the {@link SignOutCommand} command
   */
  public static SignOutCommand toCommandFromResource(RefreshTokenResource resource) {
    return new SignOutCommand(resource == null ? null : resource.refreshToken());
  }
}
