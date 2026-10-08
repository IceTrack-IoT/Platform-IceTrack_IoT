package pe.edu.upc.ice.track.platform.iam.domain.model.queries;

/**
 * Get user email by user id query
 * <p>
 *     This class represents the query to get the email address of an account. The address is not
 *     part of the {@code User} aggregate: it is owned by the account's profile.
 * </p>
 * @param userId the id of the user
 */
public record GetUserEmailByUserIdQuery(Long userId) {
}
