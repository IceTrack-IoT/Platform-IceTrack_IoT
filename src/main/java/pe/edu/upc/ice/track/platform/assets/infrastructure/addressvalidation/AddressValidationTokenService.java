package pe.edu.upc.ice.track.platform.assets.infrastructure.addressvalidation;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.AddressValidationTokenPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CoverageArea;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.VerifiedAddress;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and checks the signed token that carries a verified address to site registration.
 *
 * <p>The token is an HMAC-SHA256 JWT signed with {@code assets.address-validation.secret}, a key of
 * its own: a user authentication token can never be replayed as an address token, nor the reverse.
 * It is valid for {@value #TTL_MINUTES} minutes, bound to one owner through its subject and to one
 * purpose through its audience.</p>
 *
 * <p>The secret is Base64 and must decode to at least 32 bytes; the application refuses to start
 * otherwise. Every failed check - bad signature, wrong audience, expiry, another owner, a malformed
 * token - yields an empty result, so callers never learn why a token was rejected.</p>
 */
@Service
public class AddressValidationTokenService implements AddressValidationTokenPort {

  /** Lifetime of a validation token, in minutes. */
  static final long TTL_MINUTES = 15;

  /** Audience every address token carries and that registration requires. */
  static final String AUDIENCE = "icetrack-site-registration";

  /** HS256 requires a key of at least 256 bits. */
  static final int MIN_KEY_BYTES = 32;

  private static final String CLAIM_FORMATTED_ADDRESS = "formatted_address";
  private static final String CLAIM_PLACE_ID = "place_id";
  private static final String CLAIM_LAT = "lat";
  private static final String CLAIM_LNG = "lng";
  private static final String CLAIM_DISTRICT = "district";
  private static final String CLAIM_AREA = "area";

  private final SecretKey key;

  /**
   * Builds the service and validates the signing key once, at startup.
   *
   * @param secret the Base64-encoded signing secret; required, at least 32 decoded bytes
   * @throws IllegalStateException when the secret is missing, not Base64, or too short
   */
  public AddressValidationTokenService(@Value("${assets.address-validation.secret:}") String secret) {
    this.key = keyFrom(secret);
  }

  // inherited javadoc
  @Override
  public String issue(Long ownerId, VerifiedAddress address) {
    var now = Instant.now();
    var builder = Jwts.builder()
        .subject(ownerId.toString())
        .audience().add(AUDIENCE).and()
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofMinutes(TTL_MINUTES))))
        .id(UUID.randomUUID().toString())
        .claim(CLAIM_FORMATTED_ADDRESS, address.formattedAddress())
        .claim(CLAIM_PLACE_ID, address.placeId())
        .claim(CLAIM_LAT, address.location().latitude())
        .claim(CLAIM_LNG, address.location().longitude())
        .claim(CLAIM_AREA, address.area().name());
    if (address.district() != null) {
      builder.claim(CLAIM_DISTRICT, address.district());
    }
    return builder.signWith(key, Jwts.SIG.HS256).compact();
  }

  // inherited javadoc
  @Override
  public Optional<VerifiedAddress> verify(String token, Long ownerId) {
    if (token == null || token.isBlank() || ownerId == null) {
      return Optional.empty();
    }
    try {
      var claims = Jwts.parser()
          .verifyWith(key)
          .requireAudience(AUDIENCE)
          .requireSubject(ownerId.toString())
          .build()
          .parseSignedClaims(token)
          .getPayload();
      var placeId = claims.get(CLAIM_PLACE_ID, String.class);
      var location = new GeoLocation(
          ((Number) claims.get(CLAIM_LAT)).doubleValue(),
          ((Number) claims.get(CLAIM_LNG)).doubleValue(),
          placeId);
      return Optional.of(new VerifiedAddress(
          claims.get(CLAIM_FORMATTED_ADDRESS, String.class),
          placeId,
          location,
          claims.get(CLAIM_DISTRICT, String.class),
          CoverageArea.valueOf(claims.get(CLAIM_AREA, String.class))));
    } catch (JwtException | IllegalArgumentException | ClassCastException | NullPointerException e) {
      return Optional.empty();
    }
  }

  /**
   * Decodes and validates the signing secret.
   *
   * @param secret the Base64-encoded secret, as configured
   * @return the HMAC key built from the decoded bytes
   * @throws IllegalStateException when the secret cannot be used
   */
  static SecretKey keyFrom(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "assets.address-validation.secret is not set: define the ADDRESS_VALIDATION_SECRET environment "
              + "variable (generate one with: openssl rand -base64 32)");
    }
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(secret.trim());
    } catch (RuntimeException exception) {
      // The exception message is not chained into ours on purpose: it may quote the secret.
      throw new IllegalStateException("assets.address-validation.secret must be Base64 encoded");
    }
    if (keyBytes.length < MIN_KEY_BYTES) {
      throw new IllegalStateException(
          "assets.address-validation.secret must decode to at least %d bytes, but decodes to %d"
              .formatted(MIN_KEY_BYTES, keyBytes.length));
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
