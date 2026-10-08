package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *  Persistence entity representing an alert policy in the database.
 */
@Entity
@Table(name = "alert_policies")
@Getter
@Setter
@NoArgsConstructor
public class AlertPolicyPersistenceEntity {

  /**
   *  Primary key of the alert policy entity.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** {@code null} identifies the platform-wide default policy. */
  private Long equipmentId;

  /**
   *  Maximum allowed temperature in Celsius before an alert is triggered.
   */
  @Column(nullable = false)
  private Integer sustainedExcursionMinutes;

  /**
   *  Margin in Celsius that a reading must re-enter range by before the alert closes.
   */
  @Column(nullable = false)
  private Double hysteresisMarginCelsius;

  /**
   *  Number of missed synchronisation windows before a device is considered offline.
   */
  @Column(nullable = false)
  private Integer missedSyncWindowsForOffline;

  /**
   *  Whether this policy is currently active.
   */
  @Column(nullable = false)
  private boolean active;
}
