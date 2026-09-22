package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google;

/**
 * Immutable representation of the verified claims extracted from a Google OIDC id_token.
 *
 * <p>This payload is produced by the {@link GoogleTokenService} once the token signature,
 * issuer, audience and expiration have been validated, and is consumed by the application
 * layer to load or register a local user. It deliberately exposes only the claims the IAM
 * bounded context needs, so no provider specific type ever reaches the domain model.</p>
 *
 * @param subject       the Google unique subject identifier (the {@code sub} claim), never {@code null}
 * @param email         the email address associated with the Google account (the {@code email} claim)
 * @param emailVerified whether Google has verified the email address (the {@code email_verified} claim)
 * @param name          the display name provided by Google (the {@code name} claim), may be {@code null}
 * @param givenName     the given (first) name provided by Google, may be {@code null}
 * @param familyName    the family (last) name provided by Google, may be {@code null}
 * @param pictureUrl    the profile picture URL provided by Google, may be {@code null}
 */
public record GoogleUserInfo(
    String subject,
    String email,
    boolean emailVerified,
    String name,
    String givenName,
    String familyName,
    String pictureUrl) {

  /**
   * Resolves the best available display name for the Google account.
   *
   * <p>Falls back to the concatenation of the given and family names, and finally to the
   * local part of the email address, so callers always receive a non-blank value.</p>
   *
   * @return a non-blank display name for the account
   */
  public String displayName() {
    if (isNotBlank(name)) return name.trim();
    var composed = "%s %s".formatted(givenName == null ? "" : givenName, familyName == null ? "" : familyName).trim();
    if (isNotBlank(composed)) return composed;
    return localPartOfEmail();
  }

  /**
   * Resolves the local part of the account email, used as a last resort display name.
   *
   * @return the substring of the email before the {@code @} separator
   */
  private String localPartOfEmail() {
    var separatorIndex = email.indexOf('@');
    return separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }
}
