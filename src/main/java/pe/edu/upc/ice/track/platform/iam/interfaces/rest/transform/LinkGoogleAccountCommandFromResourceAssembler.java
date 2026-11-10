package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.LinkGoogleAccountCommand;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.LinkGoogleAccountResource;

/**
 * Assembler that converts a {@link LinkGoogleAccountResource} into a {@link LinkGoogleAccountCommand}.
 */
public class LinkGoogleAccountCommandFromResourceAssembler {

  /**
   * Converts the account linking payload into its command representation.
   *
   * @param resource the {@link LinkGoogleAccountResource} resource to convert
   * @return the {@link LinkGoogleAccountCommand} command
   */
  public static LinkGoogleAccountCommand toCommandFromResource(LinkGoogleAccountResource resource) {
    return new LinkGoogleAccountCommand(resource.idToken(), resource.password(), resource.channel().toDomain());
  }
}
