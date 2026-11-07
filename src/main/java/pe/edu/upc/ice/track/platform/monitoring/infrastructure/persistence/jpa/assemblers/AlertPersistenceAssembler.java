package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

import java.time.Duration;

/**
 * Bidirectional mapper between {@link Alert} and {@link AlertPersistenceEntity}.
 *
 * <p>{@code Temperature} is unwrapped to a plain {@code Double} column here, at the persistence
 * boundary, the same way {@code SensorReadingPersistenceAssembler} does.</p>
 *
 * <p>Alerts dismissed before {@code dismissed_at} existed stored the dismissal instant in
 * {@code resolved_at}; for those rows it is read back as the dismissal instant.</p>
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
    entity.setTriggeringTemperature(alert.getTriggeringTemperature().celsius());
    entity.setTriggeringRecordedAt(alert.getTriggeringRecordedAt());
    entity.setPeakTemperature(alert.getPeakTemperature().celsius());
    entity.setExcursionDurationSeconds(
        alert.getExcursionDuration() == null ? null : alert.getExcursionDuration().toSeconds());
    entity.setOpenedAt(alert.getOpenedAt());
    entity.setAcknowledgedByUserId(alert.getAcknowledgedBy());
    entity.setAcknowledgedAt(alert.getAcknowledgedAt());
    entity.setResolvedByUserId(alert.getResolvedBy());
    entity.setResolvedAt(alert.getResolvedAt());
    entity.setDismissedByUserId(alert.getDismissedBy());
    entity.setDismissedAt(alert.getDismissedAt());
    entity.setDismissReason(alert.getDismissReason());
    entity.setDismissEvidence(alert.getDismissEvidence());
    return entity;
  }

  public static Alert toDomainFromPersistenceEntity(AlertPersistenceEntity entity) {
    var alert = new Alert(
        entity.getEquipmentId(),
        AlertType.valueOf(entity.getType()),
        AlertSeverity.valueOf(entity.getSeverity()),
        entity.getTriggeringReadingId(),
        entity.getTriggeringTemperature(),
        entity.getTriggeringRecordedAt(),
        entity.getPeakTemperature(),
        entity.getExcursionDurationSeconds() == null
            ? null : Duration.ofSeconds(entity.getExcursionDurationSeconds()));
    alert.assignId(entity.getId());

    var status = AlertStatus.valueOf(entity.getStatus());
    var isLegacyDismissal = status == AlertStatus.DISMISSED && entity.getDismissedAt() == null;
    alert.restoreState(
        status,
        entity.getOpenedAt(),
        entity.getAcknowledgedByUserId(),
        entity.getAcknowledgedAt(),
        entity.getResolvedByUserId(),
        isLegacyDismissal ? null : entity.getResolvedAt(),
        entity.getDismissedByUserId(),
        isLegacyDismissal ? entity.getResolvedAt() : entity.getDismissedAt(),
        entity.getDismissReason(),
        entity.getDismissEvidence());
    return alert;
  }
}
