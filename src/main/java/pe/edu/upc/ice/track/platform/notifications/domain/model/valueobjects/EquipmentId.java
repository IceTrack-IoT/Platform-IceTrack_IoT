package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of an {@code Equipment} in the Monitoring and Alerting Management bounded context.
 * @param equipmentId the identifier of the equipment; must not be null
 */
@Embeddable
public record EquipmentId(Long equipmentId) {

  public EquipmentId{
    if (equipmentId == null){
      throw new IllegalArgumentException("EquipmentId cannot be null");
    }
  }
}
