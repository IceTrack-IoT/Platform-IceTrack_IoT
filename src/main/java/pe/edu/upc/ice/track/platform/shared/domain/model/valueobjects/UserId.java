package pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects;

/**
 * A value object representing a user's unique identifier in the shared kernel.
 *
 * <p>The identifier is immutable and only accepts values that can be a persisted identity: a
 * non-null, strictly positive number.</p>
 *
 * @param userId the user identifier; required and strictly positive
 */
public record UserId(Long userId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public UserId {
    if (userId == null || userId <= 0) {
      throw new IllegalArgumentException("User identifier must be a positive number");
    }
  }
}
