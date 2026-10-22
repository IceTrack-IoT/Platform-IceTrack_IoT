package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to register a new site for an owner.
 *
 * <p>The owner is never part of the payload: the command carries the username of the already
 * authenticated caller, which the command service resolves to an owner, so a client can only ever
 * create a site for itself.</p>
 *
 * @param username    username of the authenticated caller; required
 * @param name        the site name; required, at most 30 characters
 * @param address     the physical address of the site; required, at most 50 characters
 * @param contactName the person reachable on site; required, at most 30 characters
 * @param phone       the contact phone number; required
 */
public record RegisterSiteCommand(
    String username,
    String name,
    String address,
    String contactName,
    String phone) {
}