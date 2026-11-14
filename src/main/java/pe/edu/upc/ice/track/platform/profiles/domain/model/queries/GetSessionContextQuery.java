package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

/**
 * Query for everything a client shell needs right after sign-in: the signed-in account, its owner
 * profile and dashboard configuration when it has them, and how many sites and units it owns.
 *
 * @param username username of the authenticated caller; required
 */
public record GetSessionContextQuery(String username) {
}
