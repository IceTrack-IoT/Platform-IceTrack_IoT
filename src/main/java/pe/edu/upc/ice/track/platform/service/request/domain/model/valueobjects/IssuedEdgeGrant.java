package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;

import java.util.Objects;

/**
 * A freshly issued edge grant together with its plaintext token.
 *
 * <p>The only place the plaintext ever exists on the server: it is handed to the technician in the
 * response that issued the grant and never stored.</p>
 *
 * @param grant the persisted grant
 * @param token the plaintext token, {@code it_grt_} followed by 64 hexadecimal characters
 */
public record IssuedEdgeGrant(EdgeGrant grant, String token) {

  public IssuedEdgeGrant {
    Objects.requireNonNull(grant, "grant must not be null");
    Objects.requireNonNull(token, "token must not be null");
  }
}
