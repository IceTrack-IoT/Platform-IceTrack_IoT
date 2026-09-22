package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;

/**
 * JPA persistence entity for IAM roles.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class RolePersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "name", nullable = false, unique = true, length = 20)
  private Roles name;
}
