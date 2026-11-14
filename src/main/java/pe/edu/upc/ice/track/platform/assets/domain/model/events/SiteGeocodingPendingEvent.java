package pe.edu.upc.ice.track.platform.assets.domain.model.events;

/**
 * Domain event raised when a site is left {@code PENDING} because the maps service could not answer
 * right away, so the lookup has to continue in the background.
 *
 * <p>Handled by {@code assets.application.internal.eventhandlers.SiteGeocodingPendingEventHandler}
 * once the pending status is committed.</p>
 *
 * @param siteId identifier of the site waiting for its location
 */
public record SiteGeocodingPendingEvent(Long siteId) {
}
