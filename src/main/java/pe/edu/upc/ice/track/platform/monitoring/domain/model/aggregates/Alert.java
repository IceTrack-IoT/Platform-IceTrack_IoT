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
 * <p><strong>Lifecycle.</strong></p>
 * <pre>
 *   OPEN ──acknowledge──▶ ACKNOWLEDGED
 *   OPEN | ACKNOWLEDGED ──resolve──▶ RESOLVED
 *   OPEN | ACKNOWLEDGED ──dismiss──▶ DISMISSED
 * </pre>
 * <p>RESOLVED and DISMISSED are final. Since no transition can happen twice, the actor and
 * instant recorded for each one form the complete transition history of the alert:
 * {@code acknowledgedBy}/{@code acknowledgedAt}, {@code resolvedBy}/{@code resolvedAt} and
 * {@code dismissedBy}/{@code dismissedAt}, the last together with the mandatory
 * {@code dismissReason} and an optional {@code dismissEvidence}. Actors are IAM user
 * identifiers. {@code resolvedBy} stays {@code null} when the alert was resolved automatically,
 * because the readings returned within range.</p>
 *
 * <p>{@code triggeringTemperature} and {@code triggeringRecordedAt} snapshot the reading that
 * raised the alert, so it can be shown without loading that reading. {@code peakTemperature} and
 * {@code triggeringTemperature} are {@link Temperature} Value Objects wrapping {@code null} for a
 * {@code DEVICE_OFFLINE} alert, where no temperature reading is involved.</p>
 */
@Getter
public class Alert extends AbstractDomainAggregateRoot<Alert> {

  /** Longest accepted dismissal reason, in characters. */
  public static final int MAX_DISMISS_REASON_LENGTH = 500;

  /** Longest accepted dismissal evidence, in characters. */
  public static final int MAX_DISMISS_EVIDENCE_LENGTH = 1000;

  private Long id;
  private final Long equipmentId;
  private final AlertType type;
  private AlertSeverity severity;
  private AlertStatus status;
  private final Long triggeringReadingId;
  private final Temperature triggeringTemperature;
  private final LocalDateTime triggeringRecordedAt;
  private Temperature peakTemperature;
  private final Duration excursionDuration;
  private LocalDateTime openedAt;
  private Long acknowledgedBy;
  private LocalDateTime acknowledgedAt;
  private Long resolvedBy;
  private LocalDateTime resolvedAt;
  private Long dismissedBy;
  private LocalDateTime dismissedAt;
  private String dismissReason;
  private String dismissEvidence;

  /**
   * Raises a new, OPEN alert.
   *
   * @param equipmentId           identifier of the unit the alert concerns; required
   * @param type                  kind of condition raised; required
   * @param severity              how urgently the alert must be surfaced; required
   * @param triggeringReadingId   identifier of the reading that raised the alert, or {@code null}
   * @param triggeringTemperature the out-of-range value of that reading, in Celsius, or {@code null}
   * @param triggeringRecordedAt  when that reading was recorded, or {@code null}
   * @param peakTemperature       the most extreme value observed during the excursion, or {@code null}
   * @param excursionDuration     how long the excursion lasted before the alert was raised, or {@code null}
   */
  public Alert(Long equipmentId, AlertType type, AlertSeverity severity, Long triggeringReadingId,
               Double triggeringTemperature, LocalDateTime triggeringRecordedAt,
               Double peakTemperature, Duration excursionDuration) {
    this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.severity = Objects.requireNonNull(severity, "severity must not be null");
    this.triggeringReadingId = triggeringReadingId;
    this.triggeringTemperature = new Temperature(triggeringTemperature);
    this.triggeringRecordedAt = triggeringRecordedAt;
    this.peakTemperature = new Temperature(peakTemperature);
    this.excursionDuration = excursionDuration;
    this.status = AlertStatus.OPEN;
    this.openedAt = LocalDateTime.now();
  }

  /**
   * Raises a thermal excursion alert from the edge gateway's sustained-excursion event.
   *
   * <p>The gateway has already waited for the excursion to last the policy's sustained duration, so
   * the alert opens at once, {@link AlertSeverity#CRITICAL}, and is dated when the gateway detected
   * it rather than when the event reached the cloud. A detection instant reported in the future,
   * because of a drifting device clock, is replaced by the server clock.</p>
   *
   * @param equipmentId           the unit; required
   * @param triggeringReadingId   the stored reading the excursion was detected in, or {@code null}
   * @param temperatureCelsius    the out-of-range temperature the gateway reported; required
   * @param detectedAt            when the gateway detected the excursion; required
   * @param excursionDuration     the sustained duration the gateway waited for, or {@code null}
   * @return the new OPEN alert, not yet persisted
   */
  public static Alert raisedFromEdge(Long equipmentId, Long triggeringReadingId, Double temperatureCelsius,
                                     LocalDateTime detectedAt, Duration excursionDuration) {
    Objects.requireNonNull(temperatureCelsius, "temperatureCelsius must not be null");
    Objects.requireNonNull(detectedAt, "detectedAt must not be null");
    var alert = new Alert(equipmentId, AlertType.TEMPERATURE_EXCURSION, AlertSeverity.CRITICAL, triggeringReadingId,
        temperatureCelsius, detectedAt, temperatureCelsius, excursionDuration);
    var now = LocalDateTime.now();
    alert.openedAt = detectedAt.isAfter(now) ? now : detectedAt;
    return alert;
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

  /**
   * Acknowledges the alert without yet resolving the underlying condition.
   *
   * @param actorUserId IAM identifier of the user acknowledging it; required
   * @throws IllegalArgumentException when the actor is missing
   * @throws IllegalStateException    when the alert is not OPEN
   */
  public void acknowledge(Long actorUserId) {
    requireActor(actorUserId);
    if (status != AlertStatus.OPEN) {
      throw new IllegalStateException("Only an OPEN alert can be acknowledged, was " + status);
    }
    this.status = AlertStatus.ACKNOWLEDGED;
    this.acknowledgedBy = actorUserId;
    this.acknowledgedAt = LocalDateTime.now();
  }

  /**
   * Resolves the alert manually, recording who resolved it and when.
   *
   * @param actorUserId IAM identifier of the user resolving it; required
   * @throws IllegalArgumentException when the actor is missing
   * @throws IllegalStateException    when the alert is already RESOLVED or DISMISSED
   */
  public void resolve(Long actorUserId) {
    requireActor(actorUserId);
    requireActive();
    this.status = AlertStatus.RESOLVED;
    this.resolvedBy = actorUserId;
    this.resolvedAt = LocalDateTime.now();
  }

  /**
   * Resolves the alert because the stored readings show the condition stopped applying, with no
   * user involved.
   *
   * @throws IllegalStateException when the alert is already RESOLVED or DISMISSED
   */
  public void resolveAutomatically() {
    requireActive();
    this.status = AlertStatus.RESOLVED;
    this.resolvedAt = LocalDateTime.now();
  }

  /**
   * Resolves the alert because the edge gateway reported the excursion normalized: the temperature
   * stayed back within range, by the hysteresis margin, for the sustained duration.
   *
   * <p>No user is involved, so {@code resolvedBy} stays {@code null}. The resolution is dated when
   * the gateway normalized the excursion, kept between the opening and the server clock so the
   * total duration can never be negative or reach into the future. The peak the gateway reports
   * replaces the one recorded when the alert opened.</p>
   *
   * @param peakTemperatureCelsius the most extreme temperature reached during the excursion, or
   *                               {@code null} to keep the recorded peak
   * @param normalizedAt           when the gateway normalized the excursion; required
   * @throws IllegalStateException when the alert is already RESOLVED or DISMISSED
   */
  public void resolveFromEdge(Double peakTemperatureCelsius, LocalDateTime normalizedAt) {
    Objects.requireNonNull(normalizedAt, "normalizedAt must not be null");
    requireActive();
    var now = LocalDateTime.now();
    var resolution = normalizedAt.isAfter(now) ? now : normalizedAt;
    if (openedAt != null && resolution.isBefore(openedAt)) {
      resolution = openedAt;
    }
    if (peakTemperatureCelsius != null) {
      this.peakTemperature = new Temperature(peakTemperatureCelsius);
    }
    this.status = AlertStatus.RESOLVED;
    this.resolvedAt = resolution;
  }

  /**
   * Dismisses the alert as not actionable, without asserting the condition was resolved.
   *
   * @param actorUserId IAM identifier of the user dismissing it; required
   * @param reason      why the alert is not actionable, such as a planned defrost cycle; required,
   *                    at most {@value #MAX_DISMISS_REASON_LENGTH} characters
   * @param evidence    supporting evidence, such as a note or a link; optional, at most
   *                    {@value #MAX_DISMISS_EVIDENCE_LENGTH} characters
   * @throws IllegalArgumentException when the actor or the reason is missing, or a text is too long
   * @throws IllegalStateException    when the alert is already RESOLVED or DISMISSED
   */
  public void dismiss(Long actorUserId, String reason, String evidence) {
    requireActor(actorUserId);
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("A reason is required to dismiss an alert");
    }
    var trimmedReason = reason.trim();
    if (trimmedReason.length() > MAX_DISMISS_REASON_LENGTH) {
      throw new IllegalArgumentException(
          "The dismissal reason must not exceed %d characters".formatted(MAX_DISMISS_REASON_LENGTH));
    }
    var trimmedEvidence = evidence == null || evidence.isBlank() ? null : evidence.trim();
    if (trimmedEvidence != null && trimmedEvidence.length() > MAX_DISMISS_EVIDENCE_LENGTH) {
      throw new IllegalArgumentException(
          "The dismissal evidence must not exceed %d characters".formatted(MAX_DISMISS_EVIDENCE_LENGTH));
    }
    requireActive();
    this.status = AlertStatus.DISMISSED;
    this.dismissedBy = actorUserId;
    this.dismissedAt = LocalDateTime.now();
    this.dismissReason = trimmedReason;
    this.dismissEvidence = trimmedEvidence;
  }

  /**
   * Computes how long the alert stayed active.
   *
   * @return the time between opening and resolution or dismissal, or {@code null} while active
   */
  public Duration totalDuration() {
    var closedAt = resolvedAt != null ? resolvedAt : dismissedAt;
    return closedAt == null || openedAt == null ? null : Duration.between(openedAt, closedAt);
  }

  /**
   * Restores the lifecycle of an alert aggregate from persisted data, bypassing the transition
   * rules. Used exclusively by the persistence assembler.
   *
   * @param status          the persisted status; required
   * @param openedAt        when the alert was opened; required
   * @param acknowledgedBy  who acknowledged it, or {@code null}
   * @param acknowledgedAt  when it was acknowledged, or {@code null}
   * @param resolvedBy      who resolved it, or {@code null} when not resolved or resolved automatically
   * @param resolvedAt      when it was resolved, or {@code null}
   * @param dismissedBy     who dismissed it, or {@code null}
   * @param dismissedAt     when it was dismissed, or {@code null}
   * @param dismissReason   why it was dismissed, or {@code null}
   * @param dismissEvidence the evidence given for the dismissal, or {@code null}
   */
  public void restoreState(AlertStatus status, LocalDateTime openedAt,
                           Long acknowledgedBy, LocalDateTime acknowledgedAt,
                           Long resolvedBy, LocalDateTime resolvedAt,
                           Long dismissedBy, LocalDateTime dismissedAt,
                           String dismissReason, String dismissEvidence) {
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
    this.acknowledgedBy = acknowledgedBy;
    this.acknowledgedAt = acknowledgedAt;
    this.resolvedBy = resolvedBy;
    this.resolvedAt = resolvedAt;
    this.dismissedBy = dismissedBy;
    this.dismissedAt = dismissedAt;
    this.dismissReason = dismissReason;
    this.dismissEvidence = dismissEvidence;
  }

  private void requireActive() {
    if (status == AlertStatus.RESOLVED || status == AlertStatus.DISMISSED) {
      throw new IllegalStateException("Alert already closed with status " + status);
    }
  }

  private static void requireActor(Long actorUserId) {
    if (actorUserId == null) {
      throw new IllegalArgumentException("The user performing an alert transition is required");
    }
  }
}
