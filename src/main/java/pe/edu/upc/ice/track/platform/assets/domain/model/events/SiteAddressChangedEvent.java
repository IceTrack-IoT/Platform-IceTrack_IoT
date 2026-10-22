package pe.edu.upc.ice.track.platform.assets.domain.model.events;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;

/**
 * Domain event published whenever a {@link Site} is given an address that still has to be turned
 * into coordinates: when the site is registered, and every time its address changes.
 *
 * <p>Handled asynchronously by
 * {@code assets.application.internal.eventhandlers.SiteGeocodingEventHandler}, and only once the
 * transaction that changed the address has committed, so a lookup never starts for a change that
 * was rolled back.</p>
 *
 * <p>The address is carried rather than re-read so the handler can tell whether the site still has
 * this address when the lookup ends; a result for an address the site no longer holds is
 * discarded.</p>
 *
 * @param siteId  the site whose address has to be located
 * @param address the address to locate, as the site held it when the event was raised
 */
public record SiteAddressChangedEvent(Long siteId, String address) {

  /**
   * Extracts the event fields from a persisted {@link Site}.
   *
   * @param site the site (must already carry a non-null id)
   * @return the populated event
   */
  public static SiteAddressChangedEvent from(Site site) {
    return new SiteAddressChangedEvent(site.getSiteId(), site.getAddress().value());
  }
}
