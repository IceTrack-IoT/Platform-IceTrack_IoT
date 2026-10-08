package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to register a new site for an owner.
 *
 * <p>The owner is never part of the payload: it is taken from the already validated identity of
 * the caller, so a client can only ever create a site for itself.</p>
 *
 * @param ownerId     identifier of the owner the site belongs to; required
 * @param name        the site name; required, at most 30 characters
 * @param address     the physical address of the site; required, at most 50 characters
 * @param contactName the person reachable on site; required, at most 30 characters
 * @param phone       the contact phone number; required
 */
public record RegisterSiteCommand(
    Long ownerId,
    String name,
    String address,
    String contactName,
    String phone) {
}