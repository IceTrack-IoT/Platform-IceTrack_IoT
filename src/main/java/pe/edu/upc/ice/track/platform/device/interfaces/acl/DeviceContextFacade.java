package pe.edu.upc.ice.track.platform.device.interfaces.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.application.queryservices.DeviceQueryService;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceAuthenticationResult;

import java.util.Optional;
import java.time.LocalDateTime;

/** Published in-process contract through which other bounded contexts verify device credentials. */
@Service
public class DeviceContextFacade {
  private final DeviceQueryService deviceQueryService;
  private final DeviceCommandService deviceCommandService;

  public DeviceContextFacade(
      DeviceQueryService deviceQueryService, DeviceCommandService deviceCommandService) {
    this.deviceQueryService = deviceQueryService;
    this.deviceCommandService = deviceCommandService;
  }

  public Optional<DeviceAuthenticationResult> authenticate(String deviceUid, String apiKey) {
    return deviceQueryService.authenticate(deviceUid, apiKey);
  }

  public void recordActivity(Long deviceId, LocalDateTime seenAt) {
    deviceCommandService.recordActivity(deviceId, seenAt);
  }
}
