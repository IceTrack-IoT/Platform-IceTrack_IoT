package pe.edu.upc.ice.track.platform.assets.interfaces.events;

import java.time.LocalDateTime;

/**
 * Integration event published by the {@code assets} bounded context when a new equipment unit has
 * been registered and persisted.
 *
 * <p>This is the <em>published language</em> of the {@code assets} context: other bounded contexts
 * listen to this event rather than to the internal
 * {@link pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent}.</p>
 *
 * @param equipmentId the identity assigned to the newly registered unit
 * @param siteId the site the unit is installed at
 * @param uid the unit's own identifier
 * @param equipmentType the kind of refrigeration unit
 * @param minCelsius the lowest acceptable temperature, in Celsius
 * @param maxCelsius the highest acceptable temperature, in Celsius
 * @param registeredAt when the unit was catalogued
 */
public record EquipmentRegisteredIntegrationEvent(
    Long equipmentId,
    Long siteId,
    String uid,
    String equipmentType,
    Double minCelsius,
    Double maxCelsius,
    LocalDateTime registeredAt) {
}
