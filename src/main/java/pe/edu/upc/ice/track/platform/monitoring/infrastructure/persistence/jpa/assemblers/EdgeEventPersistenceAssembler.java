package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.EdgeEventPersistenceEntity;

/** Bidirectional mapper between {@link EdgeEvent} and {@link EdgeEventPersistenceEntity}. */
public final class EdgeEventPersistenceAssembler {

  private EdgeEventPersistenceAssembler() {
  }

  public static EdgeEventPersistenceEntity toPersistenceEntityFromDomain(EdgeEvent event) {
    var entity = new EdgeEventPersistenceEntity();
    entity.setId(event.getId());
    entity.setEventUid(event.getEventUid());
    entity.setDeviceId(event.getDeviceId());
    entity.setEquipmentId(event.getEquipmentId());
    entity.setType(event.getType());
    entity.setOccurredAt(event.getOccurredAt());
    entity.setTemperatureCelsius(event.getTemperatureCelsius());
    entity.setPeakTemperatureCelsius(event.getPeakTemperatureCelsius());
    entity.setResult(event.getResult());
    entity.setAlertId(event.getAlertId());
    entity.setMaintenanceWindowId(event.getMaintenanceWindowId());
    return entity;
  }

  public static EdgeEvent toDomainFromPersistenceEntity(EdgeEventPersistenceEntity entity) {
    return new EdgeEvent(
        entity.getId(),
        entity.getEventUid(),
        entity.getDeviceId(),
        entity.getEquipmentId(),
        entity.getType(),
        entity.getOccurredAt(),
        entity.getTemperatureCelsius(),
        entity.getPeakTemperatureCelsius(),
        entity.getResult(),
        entity.getAlertId(),
        entity.getMaintenanceWindowId());
  }
}
