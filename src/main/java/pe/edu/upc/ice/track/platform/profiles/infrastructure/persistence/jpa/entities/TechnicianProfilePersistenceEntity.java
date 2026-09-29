package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA persistence entity for the technician-only attributes, mapped to the
 * {@code technician_profiles} table.
 *
 * <p>Its primary key, {@code user_profile_id}, is also the foreign key to the {@code profiles}
 * row holding the shared attributes; it declares no identifier of its own.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "technician_profiles")
@PrimaryKeyJoinColumn(name = "user_profile_id")
public class TechnicianProfilePersistenceEntity extends ProfilePersistenceEntity {

  @Column(name = "speciality", nullable = false, length = 100)
  private String speciality;

  @Column(name = "certification_number", nullable = false, length = 50)
  private String certificationNumber;

  public TechnicianProfilePersistenceEntity() {
  }
}
