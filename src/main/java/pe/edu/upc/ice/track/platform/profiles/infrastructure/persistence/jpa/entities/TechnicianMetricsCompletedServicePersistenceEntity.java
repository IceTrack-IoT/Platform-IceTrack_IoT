package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA persistence entity recording that a completed service request was counted in the technician
 * metrics read model, mapped to the {@code technician_metrics_completed_services} table.
 *
 * <p>Keyed by the service request identifier, so counting the same completion twice is impossible.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "technician_metrics_completed_services")
public class TechnicianMetricsCompletedServicePersistenceEntity {

  @Id
  @Column(name = "service_request_id", nullable = false, updatable = false)
  private Long serviceRequestId;

  @Column(name = "technician_profile_id", nullable = false, updatable = false)
  private Long technicianProfileId;

  public TechnicianMetricsCompletedServicePersistenceEntity() {
  }
}
