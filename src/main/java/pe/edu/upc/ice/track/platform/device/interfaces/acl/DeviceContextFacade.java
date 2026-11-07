package pe.edu.upc.ice.track.platform.device.interfaces.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;

import java.time.LocalDateTime;

/**
 * Published in-process contract of the Device Management context.
 *
 * <p>Credential verification is not offered here: edge requests are authenticated by the edge
 * filter chain of this context, which places the device and equipment identifiers in the security
 * context before any other context sees the request.</p>
 */
@Service
public class DeviceContextFacade {
  private final DeviceCommandService deviceCommandService;

  public DeviceContextFacade(DeviceCommandService deviceCommandService) {
    this.deviceCommandService = deviceCommandService;
  }

  public void recordActivity(Long deviceId, LocalDateTime seenAt) {
    deviceCommandService.recordActivity(deviceId, seenAt);
  }
}
