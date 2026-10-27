package pe.edu.upc.ice.track.platform.device.domain.model.queries;

/** Query for the boards paired to equipment owned by the caller username. */
public record GetDevicesByEquipmentQuery(Long equipmentId, String username) {
}
