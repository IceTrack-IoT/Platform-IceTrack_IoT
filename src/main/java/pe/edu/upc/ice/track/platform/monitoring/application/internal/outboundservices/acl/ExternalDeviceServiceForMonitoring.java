package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.interfaces.acl.DeviceContextFacade;

import java.time.Instant;
import java.util.Collection;

/**
 * Outbound service (the outbound half of the Anti-Corruption Layer between {@code monitoring} and
 * {@code device}) through which a stored reading batch is reported as activity of the device that
 * sent it, so the device's {@code last_seen_at} follows its telemetry, and through which the devices
 * linked to an owner's units are counted for the dashboard.
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

  /**
   * Counts the devices linked to a set of units, for the owner dashboard.
   *
   * @param equipmentIds identifiers of the units; empty counts nothing
   * @return how many of the linked devices are paired and how many are revoked
   */
  public LinkedDeviceTotals countLinkedDevices(Collection<Long> equipmentIds) {
    var counts = deviceContextFacade.countLinkedDevices(equipmentIds);
    return new LinkedDeviceTotals(counts.paired(), counts.revoked());
  }

  /**
   * The devices linked to a set of units, counted, in this context's own terms.
   *
   * @param paired  devices paired with one of the units and able to report
   * @param revoked devices whose credential was withdrawn but that are still linked to one of the units
   */
  public record LinkedDeviceTotals(long paired, long revoked) {
  }
}
