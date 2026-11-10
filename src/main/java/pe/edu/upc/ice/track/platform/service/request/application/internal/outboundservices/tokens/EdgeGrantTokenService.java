package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.tokens;

/**
 * Outbound port generating edge grant tokens.
 *
 * <p>Implemented in the infrastructure layer, so the application layer never touches a random
 * source or a message digest directly.</p>
 */
public interface EdgeGrantTokenService {

  /**
   * Generates a new high-entropy grant token.
   *
   * @return the plaintext token and its hex SHA-256 digest, the only form ever stored
   */
  IssuedEdgeGrantToken generate();

  /**
   * A generated token.
   *
   * @param plainText the token handed to the technician once
   * @param digest    hex SHA-256 digest of {@code plainText}
   */
  record IssuedEdgeGrantToken(String plainText, String digest) {
  }
}
