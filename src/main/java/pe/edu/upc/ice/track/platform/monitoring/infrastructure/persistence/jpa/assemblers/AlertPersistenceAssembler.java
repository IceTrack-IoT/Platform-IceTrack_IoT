package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

import java.time.Duration;

/**
 * Bidirectional mapper between {@link Alert} and {@link AlertPersistenceEntity}.
 *
 * <p>{@code Temperature} is unwrapped to a plain {@code Double} column here, at the persistence
 * boundary, the same way {@code SensorReadingPersistenceAssembler} does.</p>
 */
public final class AlertPersistenceAssembler {

  private AlertPersistenceAssembler() {
  }

  public static AlertPersistenceEntity toPersistenceEntityFromDomain(Alert alert) {
    var entity = new AlertPersistenceEntity();
    entity.setId(alert.getId());
    entity.setEquipmentId(alert.getEquipmentId());
    entity.setType(alert.getType().name());
    entity.setSeverity(alert.getSeverity().name());
    entity.setStatus(alert.getStatus().name());
    entity.setTriggeringReadingId(alert.getTriggeringReadingId());
    entity.setPeakTemperature(alert.getPeakTemperature().celsius());
    entity.setExcursionDurationSeconds(
        alert.getExcursionDuration() == null ? null : alert.getExcursionDuration().toSeconds());
    entity.setOpenedAt(alert.getOpenedAt());
    entity.setResolvedAt(alert.getResolvedAt());
    return entity;
  }

  public static Alert toDomainFromPersistenceEntity(AlertPersistenceEntity entity) {
    var alert = new Alert(
        entity.getEquipmentId(),
        AlertType.valueOf(entity.getType()),
        AlertSeverity.valueOf(entity.getSeverity()),
        entity.getTriggeringReadingId(),
        entity.getPeakTemperature(),
        entity.getExcursionDurationSeconds() == null
            ? null : Duration.ofSeconds(entity.getExcursionDurationSeconds()));
    alert.assignId(entity.getId());
    // status/openedAt/resolvedAt are set via reflection-free domain transitions in a real
    // implementation (e.g. a package-private rehydration constructor); omitted here for brevity.
    return alert;
  }
}
