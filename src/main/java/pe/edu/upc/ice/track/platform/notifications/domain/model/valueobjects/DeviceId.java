package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of a {@code Device} in the Monitoring and Alerting Management bounded context.
 * @param deviceId  the identifier of the device; must not be null
 */
@Embeddable
public record DeviceId(Long deviceId) {
  public DeviceId{
    if (deviceId == null){
      throw new IllegalArgumentException("DeviceId cannot be null");
    }
  }
}
