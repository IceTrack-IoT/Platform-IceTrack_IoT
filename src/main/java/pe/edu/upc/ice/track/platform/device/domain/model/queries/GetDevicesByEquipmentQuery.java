package pe.edu.upc.ice.track.platform.device.domain.model.queries;

/** Query for the boards paired to a refrigeration unit. */
public record GetDevicesByEquipmentQuery(Long equipmentId, Long ownerId) {
}
