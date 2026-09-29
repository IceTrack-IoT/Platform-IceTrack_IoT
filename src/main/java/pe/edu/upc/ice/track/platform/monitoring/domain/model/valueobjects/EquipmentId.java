package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record EquipmentId(Long equipmentId) {

  public  EquipmentId{
    if (equipmentId == null){
      throw new IllegalArgumentException("EquipmentId cannot be null");
    }
  }
}
