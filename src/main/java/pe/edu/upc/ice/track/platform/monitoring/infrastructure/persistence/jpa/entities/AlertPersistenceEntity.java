package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class AlertPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long equipmentId;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String severity;

  @Column(nullable = false)
  private String status;

  private Long triggeringReadingId;
  private Double peakTemperature;
  private Long excursionDurationSeconds;

  @Column(nullable = false)
  private LocalDateTime openedAt;

  private LocalDateTime resolvedAt;
}
