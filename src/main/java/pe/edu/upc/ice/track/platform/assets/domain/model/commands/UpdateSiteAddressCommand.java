package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to change the address of an existing site, which restarts its geocoding.
 *
 * <p>Submitting the address the site already holds is how a client asks for another lookup after
 * a {@code FAILED} or {@code NOT_FOUND} one; it is ignored only when the site is already
 * {@code RESOLVED}.</p>
 *
 * @param siteId     identifier of the site to update; required
 * @param ownerId    identifier of the owner the site must belong to; required. Carried in the
 *                   command, not in the payload, so the ownership rule is enforced by the command
 *                   service rather than trusted to the caller
 * @param newAddress the new address; required, at most 50 characters
 */
public record UpdateSiteAddressCommand(Long siteId, Long ownerId, String newAddress) {
}
