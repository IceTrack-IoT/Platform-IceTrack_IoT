package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

/**
 * REST mirror of {@link ClientChannel}: the client application a sign-in comes from.
 */
@Schema(
    name = "ClientChannel",
    description = "Client application the request comes from: WEB admits only OWNER_ROLE accounts, "
        + "MOBILE admits only TECHNICIAN_ROLE accounts",
    example = "WEB")
public enum ClientChannelResource {
  WEB,
  MOBILE;

  /**
   * Converts this REST value into its domain counterpart.
   *
   * @return the matching {@link ClientChannel}
   */
  public ClientChannel toDomain() {
    return ClientChannel.valueOf(name());
  }
}
