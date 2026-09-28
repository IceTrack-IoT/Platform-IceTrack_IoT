package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens;

/**
 * Outbound port for bearer token issuance and validation used by IAM commands and queries.
 */
public interface TokenService {

  /**
   * Generates a token for an account.
   *
   * @param username principal username, stored as the token subject
   * @param role     the account's definitive role name, stored as the {@code role} claim
   * @return signed token value
   */
  String generateToken(String username, String role);

  /**
   * Extracts the username from a token.
   *
   * @param token token value
   * @return username embedded in the token
   */
  String getUsernameFromToken(String token);

  /**
   * Validates a token.
   *
   * @param token token value
   * @return {@code true} when token is valid; otherwise {@code false}
   */
  boolean validateToken(String token);
}
