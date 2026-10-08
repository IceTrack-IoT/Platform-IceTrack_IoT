package pe.edu.upc.ice.track.platform.assets.domain.model.events;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;

/**
 * Domain event published when a new {@link Site} is successfully registered and persisted.
 *
 * <p>Other bounded contexts must not subscribe to this event directly; it is translated into the
 * published language of the {@code assets} context by
 * {@code assets.application.internal.eventhandlers.SiteCreatedEventHandler}.</p>
 *
 * @param siteId   the identity assigned to the newly registered site
 * @param ownerId  the owner the site belongs to
 * @param name     the site name
 * @param address  the physical address of the site
 * @param contactName the person reachable on site
 * @param phone    the contact phone number
 */
public record SiteCreatedEvent(
    Long siteId,
    Long ownerId,
    String name,
    String address,
    String contactName,
    String phone) {

  /**
   * Extracts the event fields from a saved {@link Site}.
   *
   * @param site the saved site (must already carry a non-null id)
   * @return the populated event
   */
  public static SiteCreatedEvent from(Site site) {
    return new SiteCreatedEvent(
        site.getSiteId(),
        site.getOwnerId(),
        site.getName(),
        site.getAddress().value(),
        site.getContactName(),
        site.getPhone().value());
  }
}