package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Get Profile By User Id Query
 *
 * <p>Resolves the profile linked to a platform account. Used by the Profiles ACL facade to keep
 * profile provisioning idempotent across repeated sign-ins.</p>
 *
 * @param userId identifier of the account the profile belongs to
 */
public record GetProfileByUserIdQuery(UserId userId) {
}
