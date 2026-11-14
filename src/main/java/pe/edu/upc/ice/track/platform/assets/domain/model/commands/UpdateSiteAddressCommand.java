package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to move an existing site to an address verified beforehand.
 *
 * @param siteId          identifier of the site to update; required
 * @param username        username of the authenticated caller; required and resolved to an owner
 * @param validationToken the token issued by the address validation endpoint; may be {@code null}
 * @param pinLatitude     latitude of the owner's pin, or {@code null}
 * @param pinLongitude    longitude of the owner's pin, or {@code null}
 * @param expectedVersion the version the caller edited, from {@code If-Match}; {@code null} skips the check
 */
public record UpdateSiteAddressCommand(
    Long siteId,
    String username,
    String validationToken,
    Double pinLatitude,
    Double pinLongitude,
    Long expectedVersion) {

  /**
   * Creates the command without a version precondition.
   */
  public UpdateSiteAddressCommand(
      Long siteId, String username, String validationToken, Double pinLatitude, Double pinLongitude) {
    this(siteId, username, validationToken, pinLatitude, pinLongitude, null);
  }
}
