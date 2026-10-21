package pe.edu.upc.ice.track.platform.device.domain.model.queries;

/** Query for one device, scoped to an owner when invoked from the public REST API. */
public record GetDeviceByIdQuery(Long deviceId, Long ownerId) {
}
