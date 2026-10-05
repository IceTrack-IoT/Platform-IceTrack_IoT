package pe.edu.upc.ice.track.platform.assets.domain.model.events;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;

import java.time.LocalDateTime;

/**
 * Domain event published when a new {@link Equipment} is successfully registered at a site.
 *
 * <p>Other bounded contexts must not subscribe to this event directly; it is translated into the
 * published language of the {@code assets} context by
 * {@code assets.application.internal.eventhandlers.EquipmentRegisteredEventHandler}.</p>
 *
 * @param equipmentId    the identity assigned to the newly registered unit
 * @param siteId         the site the unit is installed at
 * @param uid            the unit's own identifier
 * @param name           the name assigned to the unit
 * @param equipmentType  the kind of refrigeration unit
 * @param minCelsius     the lowest acceptable temperature, in Celsius
 * @param maxCelsius     the highest acceptable temperature, in Celsius
 * @param registeredAt   when the unit was catalogued
 */
public record EquipmentRegisteredEvent(
    Long equipmentId,
    Long siteId,
    String uid,
    String name,
    EquipmentType equipmentType,
    Double minCelsius,
    Double maxCelsius,
    LocalDateTime registeredAt) {

  /**
   * Extracts the event fields from a saved {@link Equipment}.
   *
   * @param equipment the saved unit (must already carry a non-null id)
   * @return the populated event
   */
  public static EquipmentRegisteredEvent from(Equipment equipment) {
    return new EquipmentRegisteredEvent(
        equipment.getEquipmentId(),
        equipment.getSiteId(),
        equipment.getUid(),
        equipment.getName(),
        equipment.getEquipmentType(),
        equipment.getTemperatureThreshold().minCelsius(),
        equipment.getTemperatureThreshold().maxCelsius(),
        LocalDateTime.now());
  }
}