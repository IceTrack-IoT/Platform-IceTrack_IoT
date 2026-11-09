package pe.edu.upc.ice.track.platform.device.interfaces.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;

import java.time.Instant;

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

  /**
   * Records the latest contact of a paired device, which becomes its {@code last_seen_at}.
   *
   * <p>Monitoring and Alerting calls this for every reading batch it stores, with the instant the
   * platform received it. A device that is unknown or no longer paired is left alone.</p>
   *
   * @param deviceId identifier of the device
   * @param seenAt   when the platform received the device's payload
   */
  public void recordActivity(Long deviceId, Instant seenAt) {
    deviceCommandService.recordActivity(deviceId, seenAt);
  }
}
