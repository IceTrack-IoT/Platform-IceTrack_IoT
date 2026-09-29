package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA persistence entity for {@code SensorReading}.
 *
 * <p>No persistence annotation ever appears on the domain aggregate; this class, and
 * {@code SensorReadingPersistenceAssembler}, are the only places that know about the
 * relational schema.</p>
 */
@Entity
@Table(name = "sensor_readings")
@Getter
@Setter
@NoArgsConstructor
public class SensorReadingPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID readingUid;

  @Column(nullable = false)
  private Long equipmentId;

  @Column(nullable = false)
  private Long deviceId;

  private Double minTemperature;
  private Double maxTemperature;
  private Double avgTemperature;
  private Double humidity;
  private Integer sampleCount;

  @Column(nullable = false)
  private LocalDateTime recordedAt;

  @Column(nullable = false)
  private LocalDateTime receivedAt;
}
