package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA persistence entity for the owner-only attributes, mapped to the {@code owner_profiles}
 * table.
 *
 * <p>Its primary key, {@code user_profile_id}, is also the foreign key to the {@code profiles}
 * row holding the shared attributes; it declares no identifier of its own.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "owner_profiles")
@PrimaryKeyJoinColumn(name = "user_profile_id")
public class OwnerProfilePersistenceEntity extends ProfilePersistenceEntity {

  @Column(name = "ruc", nullable = false)
  private Long ruc;

  public OwnerProfilePersistenceEntity() {
  }
}
