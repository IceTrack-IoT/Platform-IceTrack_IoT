package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google;

/**
 * Outbound service (port) for validating Google OIDC id_tokens.
 *
 * <p>This port is the IAM application layer's only view of the Google identity provider.
 * Implementations are responsible for verifying the token signature against Google's
 * published JWKS, its issuer, audience and expiration, returning the verified account
 * claims when the token is valid.</p>
 *
 * <p>The adapter is implemented on top of Spring Security's
 * {@code org.springframework.security.oauth2.jwt.JwtDecoder}; no Google client SDK is
 * involved, and no Spring Security type crosses this boundary.</p>
 */
public interface GoogleTokenService {

  /**
   * Validate a Google id_token and extract its verified claims.
   *
   * @param idToken the Google id_token to be validated
   * @return a {@link GoogleUserInfo} containing the verified claims of the Google account
   * @throws IllegalArgumentException if the token is blank, malformed, expired, signed by an
   *         unexpected key, issued by an unexpected issuer, targeted at an unexpected audience,
   *         or if the associated email is missing or not verified
   */
  GoogleUserInfo verify(String idToken);
}
