package pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.google.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers the {@link JwtDecoder} used to validate Google OIDC id_tokens received through the
 * token exchange endpoint.
 *
 * <p>This replaces the former {@code com.google.api-client} based verifier with the idiomatic
 * Spring Security Resource Server primitives:</p>
 * <ul>
 *   <li>{@link NimbusJwtDecoder} resolves Google's signing keys from the published JWKS endpoint
 *       ({@code https://www.googleapis.com/oauth2/v3/certs}) and caches them, verifying the
 *       {@code RS256} signature of every submitted token.</li>
 *   <li>A {@link DelegatingOAuth2TokenValidator} enforces the standard claim set: expiration and
 *       not-before ({@link JwtTimestampValidator}), the accepted Google issuers, and the
 *       {@code aud} claim which must contain the configured OAuth client id.</li>
 * </ul>
 *
 * <p>The bean is named {@code googleJwtDecoder} and is injected by qualifier, so that it never
 * collides with the platform's own internal bearer token pipeline, which keeps using the
 * {@code tokens.jwt} HMAC service.</p>
 *
 * <p>No {@code spring.security.oauth2.resourceserver.*} property is declared on purpose: Spring
 * Boot's resource-server auto-configuration therefore stays inactive and the application's
 * {@code SecurityFilterChain} remains under the control of
 * {@code iam.infrastructure.authorization.sfs.configuration.WebSecurityConfiguration}.</p>
 */
@Configuration
public class GoogleTokenDecoderConfiguration {

  /**
   * Bean name of the Google id_token decoder, used as injection qualifier.
   */
  public static final String GOOGLE_JWT_DECODER_BEAN = "googleJwtDecoder";

  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenDecoderConfiguration.class);

  private final String jwkSetUri;
  private final String clientId;
  private final Set<String> acceptedIssuers;

  /**
   * Creates the configuration from the {@code authorization.google.*} properties.
   *
   * @param jwkSetUri Google JWKS endpoint exposing the id_token signing keys
   * @param clientId  the Google OAuth client id expected in the {@code aud} claim
   * @param issuers   comma separated list of accepted {@code iss} claim values
   */
  public GoogleTokenDecoderConfiguration(
      @Value("${authorization.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri,
      @Value("${authorization.google.client-id:}") String clientId,
      @Value("${authorization.google.issuers:https://accounts.google.com,accounts.google.com}") String issuers) {
    this.jwkSetUri = jwkSetUri;
    this.clientId = clientId == null ? "" : clientId.trim();
    this.acceptedIssuers = toIssuerSet(issuers);
  }

  /**
   * Builds the decoder that verifies Google id_tokens.
   *
   * @return a {@link JwtDecoder} restricted to Google's signing keys and to this application's audience
   */
  @Bean(GOOGLE_JWT_DECODER_BEAN)
  public JwtDecoder googleJwtDecoder() {
    var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
        .jwsAlgorithm(SignatureAlgorithm.RS256)
        .build();
    decoder.setJwtValidator(googleIdTokenValidator());
    return decoder;
  }

  /**
   * Assembles the claim validators applied to every decoded Google id_token.
   *
   * <p>The audience check is what makes a Google id_token <em>ours</em>: Google signs tokens for
   * every application, so a token whose {@code aud} does not name this client id was minted for
   * somebody else and must be refused. When no client id is configured the validator therefore
   * fails closed and rejects every token - the application still boots, but the exchange
   * endpoint stays shut until {@code authorization.google.client-id} is supplied.</p>
   *
   * @return the composed token validator
   */
  private OAuth2TokenValidator<Jwt> googleIdTokenValidator() {
    OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
    OAuth2TokenValidator<Jwt> issuerValidator = new JwtClaimValidator<Object>(
        JwtClaimNames.ISS, issuer -> issuer != null && acceptedIssuers.contains(issuer.toString()));
    if (clientId.isEmpty()) {
      LOGGER.warn("No authorization.google.client-id is configured: every Google id_token will be "
          + "rejected because its audience cannot be verified.");
      OAuth2TokenValidator<Jwt> unconfiguredAudienceValidator =
          new JwtClaimValidator<Object>(JwtClaimNames.AUD, audience -> false);
      return new DelegatingOAuth2TokenValidator<>(
          List.of(timestampValidator, issuerValidator, unconfiguredAudienceValidator));
    }
    OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
        JwtClaimNames.AUD, audience -> audience != null && audience.contains(clientId));
    return new DelegatingOAuth2TokenValidator<>(
        List.of(timestampValidator, issuerValidator, audienceValidator));
  }

  /**
   * Normalises the configured issuer list into a lookup set.
   *
   * @param issuers comma separated issuer values
   * @return the set of accepted issuer values, never {@code null}
   */
  private static Set<String> toIssuerSet(String issuers) {
    if (issuers == null || issuers.isBlank()) {
      return Set.of("https://accounts.google.com", "accounts.google.com");
    }
    return Arrays.stream(issuers.split(","))
        .map(String::trim)
        .filter(issuer -> !issuer.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }
}
