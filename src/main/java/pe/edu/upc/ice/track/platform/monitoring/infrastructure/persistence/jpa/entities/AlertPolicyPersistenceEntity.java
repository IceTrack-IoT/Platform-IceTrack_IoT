package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alert_policies")
@Getter
@Setter
@NoArgsConstructor
public class AlertPolicyPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** {@code null} identifies the platform-wide default policy. */
  private Long equipmentId;

  @Column(nullable = false)
  private Integer sustainedExcursionMinutes;

  @Column(nullable = false)
  private Double hysteresisMarginCelsius;

  @Column(nullable = false)
  private Integer missedSyncWindowsForOffline;

  @Column(nullable = false)
  private boolean active;
}
