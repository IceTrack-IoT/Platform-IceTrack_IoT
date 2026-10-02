package pe.edu.upc.ice.track.platform.iam.domain.model.queries;

/**
 * Get current user query
 * <p>
 *     This class represents the query to resolve the account of the authenticated principal.
 *     The principal is identified by its username, which is the subject of the bearer token.
 * </p>
 * @param username the username of the authenticated principal; required
 */
public record GetCurrentUserQuery(String username) {

  /**
   * Validates that a principal was actually supplied.
   */
  public GetCurrentUserQuery {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username must not be null or blank");
    }
  }
}
