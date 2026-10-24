package pe.edu.upc.ice.track.platform.device.application.queryservices;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceAuthenticationResult;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesByEquipmentQuery;

import java.util.List;
import java.util.Optional;

/** Application contract for reading and authenticating managed devices. */
public interface DeviceQueryService {
  Optional<Device> handle(GetDeviceByIdQuery query);

  List<Device> handle(GetDevicesByEquipmentQuery query);

  Optional<DeviceAuthenticationResult> authenticate(String deviceUid, String apiKey);
}
