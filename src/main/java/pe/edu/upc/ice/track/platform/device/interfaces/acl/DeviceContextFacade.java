package pe.edu.upc.ice.track.platform.device.interfaces.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;

import java.time.Instant;
import java.util.Collection;

/**
 * Published in-process contract of the Device Management context.
 *
 * <p>Credential verification is not offered here: edge requests are authenticated by the edge
 * filter chain of this context, which places the device and equipment identifiers in the security
 * context before any other context sees the request.</p>
 *
 * <p>Every parameter and return type is a primitive, a boxed primitive, an {@link Instant}, a
 * collection of those or a value record of this package such as {@link LinkedDeviceCounts}: no
 * {@code Device} aggregate, value object or repository appears in its signature.</p>
 */
@Service
public class DeviceContextFacade {
  private final DeviceCommandService deviceCommandService;
  private final DeviceRepository deviceRepository;

  public DeviceContextFacade(DeviceCommandService deviceCommandService, DeviceRepository deviceRepository) {
    this.deviceCommandService = deviceCommandService;
    this.deviceRepository = deviceRepository;
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

  /**
   * Counts the boards linked to a set of units, by whether they can still report.
   *
   * <p>Monitoring and Alerting calls this for the owner dashboard, with the units of the owner it
   * already resolved: a board has no owner of its own, so the units are what scopes the count.</p>
   *
   * @param equipmentIds identifiers of the units; {@code null} or empty counts nothing
   * @return how many of the linked boards are paired and how many are revoked
   */
  @Transactional(readOnly = true)
  public LinkedDeviceCounts countLinkedDevices(Collection<Long> equipmentIds) {
    return countLinked(equipmentIds);
  }

  /**
   * Returns the board linked to a unit, paired or revoked.
   *
   * <p>Monitoring and Alerting calls this to name the board in a connectivity change it pushes to
   * the owner's stream.</p>
   *
   * @param equipmentId identifier of the unit
   * @return the board's identifier, or {@code 0L} when the identifier is {@code null} or no board is
   *         linked to the unit
   */
  @Transactional(readOnly = true)
  public Long fetchLinkedDeviceId(Long equipmentId) {
    if (equipmentId == null) {
      return 0L;
    }
    return deviceRepository.findByEquipmentId(equipmentId).stream()
        .filter(device -> device.isLinkedToEquipment() && device.getDeviceId() != null)
        .map(device -> device.getDeviceId())
        .findFirst()
        .orElse(0L);
  }

  private LinkedDeviceCounts countLinked(Collection<Long> equipmentIds) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return new LinkedDeviceCounts(0L, 0L);
    }
    return new LinkedDeviceCounts(
        deviceRepository.countLinkedToEquipmentByStatus(equipmentIds, DeviceStatus.PAIRED),
        deviceRepository.countLinkedToEquipmentByStatus(equipmentIds, DeviceStatus.REVOKED));
  }
}
