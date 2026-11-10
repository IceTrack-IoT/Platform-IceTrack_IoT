package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to verify an address before a site is saved.
 *
 * @param username the authenticated caller; required, resolved to an owner by the command service
 * @param address  the free-form address as the owner typed it or picked it; required
 * @param placeId  the provider's place identifier, when the owner picked a suggestion; may be {@code null}
 */
public record ValidateAddressCommand(String username, String address, String placeId) {
}
