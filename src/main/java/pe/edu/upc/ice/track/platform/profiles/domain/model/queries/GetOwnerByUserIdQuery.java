package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Query to retrieve the owner bound to a platform account.
 *
 * @param userId the identifier of the account
 */
public record GetOwnerByUserIdQuery(UserId userId) {
}
