package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to register a new site at an address verified beforehand.
 *
 * @param username        username of the authenticated caller; required. Resolved to an owner by the
 *                        command service, never taken from the payload
 * @param name            the site name; required, at most 30 characters
 * @param contactName     the person reachable on site; required, at most 30 characters
 * @param phone           the contact phone number; required
 * @param validationToken the token issued by the address validation endpoint; may be {@code null}
 * @param pinLatitude     latitude of the owner's pin, or {@code null} to use the verified point
 * @param pinLongitude    longitude of the owner's pin, or {@code null} to use the verified point
 */
public record RegisterSiteCommand(
    String username,
    String name,
    String contactName,
    String phone,
    String validationToken,
    Double pinLatitude,
    Double pinLongitude) {
}
