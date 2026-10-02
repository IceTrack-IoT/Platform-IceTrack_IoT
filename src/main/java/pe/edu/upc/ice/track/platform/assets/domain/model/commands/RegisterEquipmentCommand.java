package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;

/**
 * Command to register a new refrigeration unit at a site.
 *
 * @param ownerId             identifier of the owner the site's site must belong to; required
 * @param siteId              identifier of the site the unit is installed at; required
 * @param uid                 the unit's own identifier, unique across the platform; required
 * @param name                the name assigned to the unit; required
 * @param equipmentType       the kind of refrigeration unit; required
 * @param minCelsius          the lowest acceptable temperature in Celsius; required
 * @param maxCelsius          the highest acceptable temperature in Celsius; required
 * @param reminderIntervalDays the preventive maintenance interval in days; required and positive
 */
public record RegisterEquipmentCommand(
    Long ownerId,
    Long siteId,
    String uid,
    String name,
    EquipmentType equipmentType,
    Double minCelsius,
    Double maxCelsius,
    Integer reminderIntervalDays) {
}