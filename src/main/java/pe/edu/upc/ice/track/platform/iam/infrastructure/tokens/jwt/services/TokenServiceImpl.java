package pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.jwt.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Token service implementation for JWT tokens.
 * This class is responsible for generating and validating JWT tokens.
 * It uses the secret and expiration minutes from the application.properties file.
 * Access tokens are kept short-lived because they are validated statelessly and therefore cannot
 * be revoked; sessions are extended through refresh tokens instead.
 */
@Service
@Slf4j
public class TokenServiceImpl implements BearerTokenService {
  private static final String AUTHORIZATION_PARAMETER_NAME = "Authorization";
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";
  private static final String ROLE_CLAIM_NAME = "role";

  private static final int TOKEN_BEGIN_INDEX = 7;


  @Value("${authorization.jwt.secret}")
  private String secret;

  @Value("${authorization.jwt.expiration.minutes}")
  private int expirationMinutes;

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
   * This method generates a JWT token from a username, a role and a secret.
   * It uses the default expiration minutes from the application.properties file.
   * @param username the username
   * @param role the role name; the claim is omitted when {@code null}
   * @return String the JWT token
   */
  private String buildTokenWithDefaultParameters(String username, String role) {
    var issuedAt = new Date();
    var expiration = DateUtils.addMinutes(issuedAt, expirationMinutes);
    var key = getSigningKey();
    var builder = Jwts.builder()
        .subject(username)
        .issuedAt(issuedAt)
        .expiration(expiration);
    if (role != null) {
      builder.claim(ROLE_CLAIM_NAME, role);
    }
    return builder.signWith(key).compact();
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
   * This method validates a JWT token
   * @param token the token
   * @return boolean true if the token is valid, false otherwise
   */
  @Override
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      log.info("Token is valid");
      return true;
    }  catch (SignatureException e) {
      log.error("Invalid JSON Web Token Signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.error("Invalid JSON Web Token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JSON Web Token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("JSON Web Token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JSON Web Token claims string is empty: {}", e.getMessage());
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
    final Claims claims = extractAllClaims(token);
    return claimsResolvers.apply(claims);
  }

  /**
   * Extract all claims from a token
   * @param token the token
   * @return Claims the claims
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  /**
   * Get the signing key
   * @return SecretKey the signing key
   */
  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
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
