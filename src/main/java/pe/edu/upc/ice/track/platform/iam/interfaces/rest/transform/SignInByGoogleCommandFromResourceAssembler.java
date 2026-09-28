package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByGoogleCommand;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithGoogleResource;

/**
 * Assembler that converts a {@link SignInWithGoogleResource} into a {@link SignInByGoogleCommand}.
 */
public class SignInByGoogleCommandFromResourceAssembler {

  /**
   * Converts the Google verification payload into its command representation.
   *
   * @param resource the {@link SignInWithGoogleResource} resource to convert
   * @return the {@link SignInByGoogleCommand} command
   */
  public static SignInByGoogleCommand toCommandFromResource(SignInWithGoogleResource resource) {
    return new SignInByGoogleCommand(resource.idToken());
  }
}
