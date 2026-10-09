package pe.edu.upc.ice.track.platform.assets.interfaces.events;

/**
 * Integration event published by the {@code assets} bounded context when a new site has been
 * created and persisted.
 *
 * <p>This is the <em>published language</em> of the {@code assets} context: other bounded contexts
 * listen to this event rather than to the internal
 * {@link pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent}.</p>
 *
 * @param siteId the identity assigned to the newly registered site
 * @param ownerId the owner the site belongs to
 * @param name the site name
 */
public record SiteCreatedIntegrationEvent(Long siteId, Long ownerId, String name) {
}
