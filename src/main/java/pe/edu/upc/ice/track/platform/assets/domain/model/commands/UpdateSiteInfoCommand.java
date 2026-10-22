package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to replace the editable details of an existing site.
 *
 * @param siteId      identifier of the site to update; required
 * @param username    username of the authenticated caller; required. Carried in the command, not
 *                    in the payload, and resolved to an owner by the command service, so the
 *                    ownership rule is enforced there rather than trusted to the caller
 * @param name        the new site name; required
 * @param address     the new address; required
 * @param contactName the new contact name; required
 * @param phone       the new contact phone number; required
 */
public record UpdateSiteInfoCommand(
    Long siteId,
    String username,
    String name,
    String address,
    String contactName,
    String phone) {
}