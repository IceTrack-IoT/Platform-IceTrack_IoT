package pe.edu.upc.ice.track.platform.device.domain.model.queries;

/** Query for one device, scoped to the owner represented by the caller username. */
public record GetDeviceByIdQuery(Long deviceId, String username) {
}
