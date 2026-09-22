package pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.google.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleTokenService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleUserInfo;
import pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.google.configuration.GoogleTokenDecoderConfiguration;

/**
 * Infrastructure adapter that validates Google OIDC id_tokens with Spring Security's
 * {@link JwtDecoder}.
 *
 * <p>The decoder injected here is the {@code googleJwtDecoder} declared by
 * {@link GoogleTokenDecoderConfiguration}: a {@code NimbusJwtDecoder} bound to Google's JWKS
 * endpoint and to the configured audience. This adapter therefore only has to translate the
 * decoded {@link Jwt} into the port's {@link GoogleUserInfo} payload and to enforce the two
 * application level rules that are not part of JWT validation: the token must carry an email,
 * and that email must be verified by Google.</p>
 *
 * <p>Every failure is surfaced as an {@link IllegalArgumentException} so that the application
 * layer can reject the exchange without ever seeing a Spring Security type.</p>
 */
@Service
@Slf4j
public class GoogleTokenServiceImpl implements GoogleTokenService {

  private static final String EMAIL_CLAIM = "email";
  private static final String EMAIL_VERIFIED_CLAIM = "email_verified";
  private static final String NAME_CLAIM = "name";
  private static final String GIVEN_NAME_CLAIM = "given_name";
  private static final String FAMILY_NAME_CLAIM = "family_name";
  private static final String PICTURE_CLAIM = "picture";

  private static final String INVALID_TOKEN_MESSAGE = "Invalid Google id_token";

  private final JwtDecoder googleJwtDecoder;
  private final boolean requireVerifiedEmail;

  /**
   * Creates the adapter.
   *
   * @param googleJwtDecoder     the decoder validating Google id_tokens
   * @param requireVerifiedEmail whether an unverified Google email must be rejected
   */
  public GoogleTokenServiceImpl(
      @Qualifier(GoogleTokenDecoderConfiguration.GOOGLE_JWT_DECODER_BEAN) JwtDecoder googleJwtDecoder,
      @Value("${authorization.google.require-verified-email:true}") boolean requireVerifiedEmail) {
    this.googleJwtDecoder = googleJwtDecoder;
    this.requireVerifiedEmail = requireVerifiedEmail;
  }

  // inherited javadoc
  @Override
  public GoogleUserInfo verify(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException("Google id_token must not be null or blank");
    }

    Jwt jwt;
    try {
      jwt = googleJwtDecoder.decode(idToken.trim());
    } catch (JwtException exception) {
      log.warn("Rejected Google id_token: {}", exception.getMessage());
      throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
    }

    var subject = jwt.getClaimAsString(JwtClaimNames.SUB);
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Google id_token does not carry a subject claim");
    }

    var email = jwt.getClaimAsString(EMAIL_CLAIM);
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Google id_token does not carry an email claim");
    }

    var emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean(EMAIL_VERIFIED_CLAIM));
    if (requireVerifiedEmail && !emailVerified) {
      throw new IllegalArgumentException("Google account email is not verified");
    }

    return new GoogleUserInfo(
        subject,
        email.trim(),
        emailVerified,
        jwt.getClaimAsString(NAME_CLAIM),
        jwt.getClaimAsString(GIVEN_NAME_CLAIM),
        jwt.getClaimAsString(FAMILY_NAME_CLAIM),
        jwt.getClaimAsString(PICTURE_CLAIM));
  }
}
