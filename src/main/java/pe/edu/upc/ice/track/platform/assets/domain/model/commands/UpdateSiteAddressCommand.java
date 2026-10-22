package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to change the address of an existing site, which restarts its geocoding.
 *
 * <p>Submitting the address the site already holds is how a client asks for another lookup after
 * a {@code FAILED} or {@code NOT_FOUND} one; it is ignored only when the site is already
 * {@code RESOLVED}.</p>
 *
 * @param siteId     identifier of the site to update; required
 * @param username   username of the authenticated caller; required. Carried in the command, not in
 *                   the payload, and resolved to an owner by the command service, so the ownership
 *                   rule is enforced there rather than trusted to the caller
 * @param newAddress the new address; required, at most 50 characters
 */
public record UpdateSiteAddressCommand(Long siteId, String username, String newAddress) {
}
