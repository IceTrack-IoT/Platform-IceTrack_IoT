package pe.edu.upc.ice.track.platform.assets.interfaces.events;

import java.time.LocalDateTime;

/**
 * Integration event published by the {@code assets} bounded context when the acceptable temperature
 * band of an equipment unit changes.
 *
 * <p>This is the <em>published language</em> of the {@code assets} context: other bounded contexts
 * (notably Monitoring and Alerting) listen to this event rather than to the internal
 * {@link pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent}.</p>
 *
 * @param equipmentId the unit whose threshold changed
 * @param siteId the site the unit is installed at
 * @param minCelsius the new lowest acceptable temperature, in Celsius
 * @param maxCelsius the new highest acceptable temperature, in Celsius
 * @param changedAt when the change was applied
 */
public record TemperatureThresholdUpdatedIntegrationEvent(
    Long equipmentId,
    Long siteId,
    Double minCelsius,
    Double maxCelsius,
    LocalDateTime changedAt) {
}
