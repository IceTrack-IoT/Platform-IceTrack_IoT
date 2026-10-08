package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of an equipment.
 * @param equipmentId the identifier of the equipment, must not be null
 */
@Embeddable
public record EquipmentId(Long equipmentId) {

  public  EquipmentId{
    if (equipmentId == null){
      throw new IllegalArgumentException("EquipmentId cannot be null");
    }
  }
}
