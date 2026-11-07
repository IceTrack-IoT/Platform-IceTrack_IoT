package pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.jwt.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.jwt.BearerTokenService;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Token service implementation for JWT tokens.
 * This class is responsible for generating and validating JWT tokens.
 * Access tokens are kept short-lived because they are validated statelessly and therefore cannot
 * be revoked; sessions are extended through refresh tokens instead.
 *
 * <p><strong>Key handling.</strong> {@code authorization.jwt.secret} is
 * Base64 and is decoded to raw key bytes, rather than used as the bytes of its own text. The
 * decoded key must hold at least 64 bytes (512 bits, RFC 7518 §3.2 for HS512), and the application
 * refuses to start otherwise: there is no committed fallback secret, so a missing, blank, malformed
 * or short secret fails fast at startup instead of signing tokens with a key anyone can read.
 * Tokens are always signed with HS512, and the parser rejects any other algorithm.</p>
 */
@Service
@Slf4j
public class TokenServiceImpl implements BearerTokenService {
  private static final String AUTHORIZATION_PARAMETER_NAME = "Authorization";
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";
  private static final String ROLE_CLAIM_NAME = "role";

  private static final int TOKEN_BEGIN_INDEX = 7;

  /** HS512 requires a key of at least 512 bits. */
  static final int MIN_KEY_BYTES = 64;

  private static final MacAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.HS512;

  private final SecretKey signingKey;
  private final int expirationMinutes;

  /**
   * Builds the service and validates the signing key once, at startup.
   *
   * @param secret            the Base64-encoded signing secret; required, at least 64 decoded bytes
   * @param expirationMinutes the access token lifetime, in minutes; strictly positive
   * @throws IllegalStateException when the secret is missing, blank, not Base64 or too short, or
   *                               the lifetime is not positive, which aborts application startup
   */
  public TokenServiceImpl(
      @Value("${authorization.jwt.secret:}") String secret,
      @Value("${authorization.jwt.expiration.minutes}") int expirationMinutes) {
    this.signingKey = signingKeyFrom(secret);
    if (expirationMinutes <= 0) {
      throw new IllegalStateException("authorization.jwt.expiration.minutes must be a positive number of minutes");
    }
    this.expirationMinutes = expirationMinutes;
  }

  /**
   * Decodes and validates the signing secret.
   *
   * @param secret the Base64-encoded secret, as configured
   * @return the HMAC-SHA key built from the decoded bytes
   * @throws IllegalStateException when the secret cannot be used for HS512
   */
  static SecretKey signingKeyFrom(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "authorization.jwt.secret is not set: define the JWT_SECRET environment variable "
              + "(generate one with: openssl rand -base64 64 | tr -d '\\n')");
    }
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(secret.trim());
    } catch (RuntimeException exception) {
      // The exception message is not chained into ours on purpose: it may quote the secret.
      throw new IllegalStateException("authorization.jwt.secret must be Base64 encoded");
    }
    if (keyBytes.length < MIN_KEY_BYTES) {
      throw new IllegalStateException(
          "authorization.jwt.secret must decode to at least %d bytes for HS512, but decodes to %d"
              .formatted(MIN_KEY_BYTES, keyBytes.length));
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * This method generates a JWT token from an authentication object
   * @param authentication the authentication object
   * @return String the JWT token
   * @see Authentication
   */
  @Override
  public String generateToken(Authentication authentication) {
    var role = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse(null);
    return buildTokenWithDefaultParameters(authentication.getName(), role);
  }

  /**
   * This method generates a JWT token from a username and the account's definitive role
   * @param username the username
   * @param role the role name, stored as the {@code role} claim
   * @return String the JWT token
   */
  @Override
  public String generateToken(String username, String role) {
    return buildTokenWithDefaultParameters(username, role);
  }

  /**
   * Generates an HS512-signed JWT token from a username and a role, using the configured lifetime.
   * @param username the username
   * @param role the role name; the claim is omitted when {@code null}
   * @return String the JWT token
   */
  private String buildTokenWithDefaultParameters(String username, String role) {
    var issuedAt = new Date();
    var expiration = DateUtils.addMinutes(issuedAt, expirationMinutes);
    var builder = Jwts.builder()
        .subject(username)
        .issuedAt(issuedAt)
        .expiration(expiration);
    if (role != null) {
      builder.claim(ROLE_CLAIM_NAME, role);
    }
    return builder.signWith(signingKey, SIGNATURE_ALGORITHM).compact();
  }

  /**
   * This method extracts the username from a JWT token
   * @param token the token
   * @return String the username
   */
  @Override
  public String getUsernameFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * This method validates a JWT token.
   *
   * <p>Rejections are logged at debug level by exception type only: neither the token nor the
   * parser's message (which may quote parts of the token) is ever written to the log.</p>
   *
   * @param token the token
   * @return boolean true if the token is valid, false otherwise
   */
  @Override
  public boolean validateToken(String token) {
    try {
      parse(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.debug("Rejected bearer token: expired");
    } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
      log.debug("Rejected bearer token: {}", e.getClass().getSimpleName());
    }
    return false;
  }

  /**
   * Extract a claim from a token
   * @param token the token
   * @param claimsResolvers the claims resolver
   * @param <T> the type of the claim
   * @return T the claim
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
    return claimsResolvers.apply(parse(token).getPayload());
  }

  /**
   * Verifies the signature and expiry of a token, and that it was signed with HS512.
   *
   * @param token the compact JWS
   * @return the parsed, verified token
   * @throws JwtException when the token is invalid, expired or signed with another algorithm
   */
  private Jws<Claims> parse(String token) {
    var jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
    if (!SIGNATURE_ALGORITHM.getId().equals(jws.getHeader().getAlgorithm())) {
      throw new UnsupportedJwtException("Unexpected JWT signature algorithm");
    }
    return jws;
  }

  private boolean isTokenPresentIn(String authorizationParameter) {
    return StringUtils.hasText(authorizationParameter);
  }

  private boolean isBearerTokenIn(String authorizationParameter) {
    return authorizationParameter.startsWith(BEARER_TOKEN_PREFIX);
  }

  private String extractTokenFrom(String authorizationHeaderParameter) {
    return authorizationHeaderParameter.substring(TOKEN_BEGIN_INDEX);
  }

  private String getAuthorizationParameterFrom(HttpServletRequest request) {
    return request.getHeader(AUTHORIZATION_PARAMETER_NAME);
  }

  @Override
  public String getBearerTokenFrom(HttpServletRequest request) {
    String parameter = getAuthorizationParameterFrom(request);
    if (isTokenPresentIn(parameter) && isBearerTokenIn(parameter)) return extractTokenFrom(parameter);
    return null;
  }

}
