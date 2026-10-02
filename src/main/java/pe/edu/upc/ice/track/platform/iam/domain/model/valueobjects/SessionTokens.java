package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

/**
 * Credentials of an authenticated session.
 *
 * @param accessToken  the short-lived bearer JWT sent on every API call; required
 * @param refreshToken the opaque, single-use token exchanged for a new session; required
 */
public record SessionTokens(String accessToken, String refreshToken) {

  /**
   * Validates that both tokens were actually issued.
   */
  public SessionTokens {
    requireText(accessToken, "accessToken");
    requireText(refreshToken, "refreshToken");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be null or blank".formatted(field));
    }
  }
}
