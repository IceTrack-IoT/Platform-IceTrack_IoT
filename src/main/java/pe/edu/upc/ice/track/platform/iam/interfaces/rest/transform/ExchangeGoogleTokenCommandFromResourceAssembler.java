package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.ExchangeGoogleTokenCommand;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithGoogleResource;

/**
 * Assembler that converts a {@link SignInWithGoogleResource} into an
 * {@link ExchangeGoogleTokenCommand}.
 */
public class ExchangeGoogleTokenCommandFromResourceAssembler {

  /**
   * Converts the token exchange payload into its command representation.
   *
   * @param resource the {@link SignInWithGoogleResource} resource to convert
   * @return the {@link ExchangeGoogleTokenCommand} command
   */
  public static ExchangeGoogleTokenCommand toCommandFromResource(SignInWithGoogleResource resource) {
    return new ExchangeGoogleTokenCommand(resource.idToken(), resource.requestedRole());
  }
}
