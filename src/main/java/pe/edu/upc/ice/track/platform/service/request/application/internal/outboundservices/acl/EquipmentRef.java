package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl;

/**
 * What this context needs to know about a unit of the {@code assets} context: that it exists, who
 * owns it and where it is installed.
 *
 * @param equipmentId identifier of the unit
 * @param ownerId     identifier of the owner profile the unit belongs to
 * @param siteId      identifier of the site the unit is installed at
 */
public record EquipmentRef(Long equipmentId, Long ownerId, Long siteId) {
}
