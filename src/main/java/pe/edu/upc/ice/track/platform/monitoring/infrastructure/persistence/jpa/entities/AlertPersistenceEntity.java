package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 *  Persistence entity representing an alert in the system.
 *  This entity is mapped to the "alerts" table in the database and contains information about the alert,
 *  including its type, severity, status, and associated equipment.
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class AlertPersistenceEntity {

  /**
   *  The unique identifier for the alert.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   *  The identifier of the equipment associated with the alert.
   */
  @Column(nullable = false)
  private Long equipmentId;

  /**
   *  The type of the alert (e.g., temperature excursion, device offline).
   */
  @Column(nullable = false)
  private String type;

  /**
   *  The severity level of the alert (e.g., info, warning, critical).
   */
  @Column(nullable = false)
  private String severity;

  /**
   *  The current status of the alert (e.g., open, acknowledged, resolved, dismissed).
   */
  @Column(nullable = false)
  private String status;
  /**
   *  The identifier of the sensor reading that triggered the alert.
   */
  private Long triggeringReadingId;
  /**
   *  The out-of-range value of the triggering reading, in Celsius. Null for a DEVICE_OFFLINE alert
   *  and for alerts raised before this snapshot was recorded.
   */
  private Double triggeringTemperature;
  /**
   *  When the triggering reading was recorded. Null for a DEVICE_OFFLINE alert and for alerts
   *  raised before this snapshot was recorded.
   */
  private Instant triggeringRecordedAt;
  /**
   *  The peak temperature observed during the excursion, in Celsius. This field is null for a DEVICE_OFFLINE alert.
   */
  private Double peakTemperature;
  /**
   *  The duration of the excursion in seconds. This field is null for a DEVICE_OFFLINE alert.
   */
  private Long excursionDurationSeconds;

  /**
   *  The timestamp when the alert was raised.
   */
  @Column(nullable = false)
  private Instant openedAt;

  /**
   *  IAM identifier of the user who acknowledged the alert.
   */
  private Long acknowledgedByUserId;

  private Instant acknowledgedAt;

  /**
   *  IAM identifier of the user who resolved the alert; null when it was resolved automatically.
   */
  private Long resolvedByUserId;

  private Instant resolvedAt;

  /**
   *  IAM identifier of the user who dismissed the alert.
   */
  private Long dismissedByUserId;

  private Instant dismissedAt;

  @Column(length = 500)
  private String dismissReason;

  @Column(length = 1000)
  private String dismissEvidence;
}
