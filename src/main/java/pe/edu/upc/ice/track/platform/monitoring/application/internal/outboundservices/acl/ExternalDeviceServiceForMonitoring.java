package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.interfaces.acl.DeviceContextFacade;

import java.time.Instant;

/**
 * Outbound service (the outbound half of the Anti-Corruption Layer between {@code monitoring} and
 * {@code device}) through which a stored reading batch is reported as activity of the device that
 * sent it, so the device's {@code last_seen_at} follows its telemetry.
 *
 * <p>The only class in {@code monitoring} that knows a {@code device} context exists. It talks to it
 * exclusively through {@link DeviceContextFacade}.</p>
 */
@Service
public class ExternalDeviceServiceForMonitoring {

  private final DeviceContextFacade deviceContextFacade;

  public ExternalDeviceServiceForMonitoring(DeviceContextFacade deviceContextFacade) {
    this.deviceContextFacade = deviceContextFacade;
  }

  /**
   * Records that a device was heard from. A device that is unknown or no longer paired is left alone.
   *
   * @param deviceId identifier of the device; ignored when {@code null}
   * @param seenAt   when the platform received its payload; ignored when {@code null}
   */
  public void recordDeviceActivity(Long deviceId, Instant seenAt) {
    if (deviceId == null || seenAt == null) {
      return;
    }
    deviceContextFacade.recordActivity(deviceId, seenAt);
  }
}
