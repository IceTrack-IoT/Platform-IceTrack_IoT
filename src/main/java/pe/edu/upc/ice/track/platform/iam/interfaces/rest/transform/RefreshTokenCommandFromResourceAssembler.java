package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.RefreshTokenCommand;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.RefreshTokenResource;

/**
 * Assembler that converts a {@link RefreshTokenResource} into a {@link RefreshTokenCommand}.
 */
public class RefreshTokenCommandFromResourceAssembler {

  /**
   * Converts the refresh payload into its command representation.
   *
   * @param resource the {@link RefreshTokenResource} resource to convert
   * @return the {@link RefreshTokenCommand} command
   */
  public static RefreshTokenCommand toCommandFromResource(RefreshTokenResource resource) {
    return new RefreshTokenCommand(resource.refreshToken());
  }
}
