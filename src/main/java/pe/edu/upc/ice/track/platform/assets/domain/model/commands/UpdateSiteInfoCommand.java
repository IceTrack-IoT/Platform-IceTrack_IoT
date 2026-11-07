package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to replace the editable details of an existing site. The address is not part of it:
 * it changes only through {@link UpdateSiteAddressCommand}, with a verified token.
 *
 * @param siteId      identifier of the site to update; required
 * @param username    username of the authenticated caller; required
 * @param name        the new site name; required
 * @param contactName the new contact name; required
 * @param phone       the new contact phone number; required
 */
public record UpdateSiteInfoCommand(
    Long siteId,
    String username,
    String name,
    String contactName,
    String phone) {
}
