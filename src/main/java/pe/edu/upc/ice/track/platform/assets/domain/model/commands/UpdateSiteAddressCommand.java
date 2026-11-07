package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to move an existing site to an address verified beforehand.
 *
 * @param siteId          identifier of the site to update; required
 * @param username        username of the authenticated caller; required and resolved to an owner
 * @param validationToken the token issued by the address validation endpoint; may be {@code null}
 * @param pinLatitude     latitude of the owner's pin, or {@code null}
 * @param pinLongitude    longitude of the owner's pin, or {@code null}
 */
public record UpdateSiteAddressCommand(
    Long siteId,
    String username,
    String validationToken,
    Double pinLatitude,
    Double pinLongitude) {
}
