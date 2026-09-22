package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithLocalResource;

/**
 * Assembler that converts a {@link SignInWithLocalResource} into a {@link SignInByLocalCommand}.
 */
public class SignInByLocalCommandFromResourceAssembler {

  /**
   * Converts the local sign-in payload into its command representation.
   *
   * @param resource the {@link SignInWithLocalResource} resource to convert
   * @return the {@link SignInByLocalCommand} command
   */
  public static SignInByLocalCommand toCommandFromResource(SignInWithLocalResource resource) {
    return new SignInByLocalCommand(resource.username(), resource.password());
  }
}
