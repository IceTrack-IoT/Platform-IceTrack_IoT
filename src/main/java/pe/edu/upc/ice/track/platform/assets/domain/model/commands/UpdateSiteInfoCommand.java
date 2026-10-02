package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to replace the editable details of an existing site.
 *
 * @param siteId      identifier of the site to update; required
 * @param ownerId     identifier of the owner the site must belong to; required. Carried in the
 *                    command, not in the payload, so the ownership rule can be enforced by the
 *                    command service rather than trusted to the caller
 * @param name        the new site name; required
 * @param address     the new address; required
 * @param contactName the new contact name; required
 * @param phone       the new contact phone number; required
 */
public record UpdateSiteInfoCommand(
    Long siteId,
    Long ownerId,
    String name,
    String address,
    String contactName,
    String phone) {
}