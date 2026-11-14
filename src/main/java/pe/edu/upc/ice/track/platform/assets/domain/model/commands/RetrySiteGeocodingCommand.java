package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command, issued by the owner, to locate a site again from its current address.
 *
 * @param siteId   identifier of the site to locate; required
 * @param username username of the authenticated caller; required and resolved to an owner
 */
public record RetrySiteGeocodingCommand(Long siteId, String username) {
}
