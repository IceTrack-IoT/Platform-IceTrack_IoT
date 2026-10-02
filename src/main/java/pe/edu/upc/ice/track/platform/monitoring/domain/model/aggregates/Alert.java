package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertRaisedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Alert aggregate root of the {@code monitoring} bounded context.
 *
 * <p>Owns the lifecycle of a thermal excursion or device-silence condition, from the moment it
 * is raised until it is acknowledged, resolved or dismissed. Referenced by identifier only
 * ({@code triggeringReadingId}, {@code equipmentId}) — this aggregate never holds a direct
 * reference to a {@link SensorReading} instance or to an Asset Management equipment.</p>
 *
 * <p>{@code peakTemperature} is a {@link Temperature} Value Object, {@code null} for a
 * {@code DEVICE_OFFLINE} alert where no temperature reading is involved.</p>
 */
@Getter
public class Alert extends AbstractDomainAggregateRoot<Alert> {

  private Long id;
  private final Long equipmentId;
  private final AlertType type;
  private AlertSeverity severity;
  private AlertStatus status;
  private final Long triggeringReadingId;
  private final Temperature peakTemperature;
  private final Duration excursionDuration;
  private final LocalDateTime openedAt;
  private LocalDateTime resolvedAt;


  public Alert(Long equipmentId, AlertType type, AlertSeverity severity, Long triggeringReadingId,
               Double peakTemperature, Duration excursionDuration) {
    this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.severity = Objects.requireNonNull(severity, "severity must not be null");
    this.triggeringReadingId = triggeringReadingId;
    this.peakTemperature = new Temperature(peakTemperature);
    this.excursionDuration = excursionDuration;
    this.status = AlertStatus.OPEN;
    this.openedAt = LocalDateTime.now();
  }

  public void assignId(Long id) {
    this.id = id;
  }

  /**
   * Signals that this alert has just been raised and persisted.
   *
   * <p>Registers an {@link AlertRaisedEvent} so the infrastructure can publish the corresponding
   * integration event to Notifications after the transaction commits.</p>
   */
  public void onRaised() {
    registerDomainEvent(new AlertRaisedEvent(id, equipmentId, type, severity, peakTemperature.celsius()));
  }

  /** Acknowledges the alert without yet resolving the underlying condition. */
  public void acknowledge() {
    if (status != AlertStatus.OPEN) {
      throw new IllegalStateException("Only an OPEN alert can be acknowledged, was " + status);
    }
    this.status = AlertStatus.ACKNOWLEDGED;
  }

  /** Resolves the alert, recording the moment the condition stopped applying. */
  public void resolve() {
    if (status == AlertStatus.RESOLVED || status == AlertStatus.DISMISSED) {
      throw new IllegalStateException("Alert already closed with status " + status);
    }
    this.status = AlertStatus.RESOLVED;
    this.resolvedAt = LocalDateTime.now();
  }

  /** Dismisses the alert as not actionable, without asserting the condition was resolved. */
  public void dismiss() {
    if (status == AlertStatus.RESOLVED || status == AlertStatus.DISMISSED) {
      throw new IllegalStateException("Alert already closed with status " + status);
    }
    this.status = AlertStatus.DISMISSED;
    this.resolvedAt = LocalDateTime.now();
  }
}
