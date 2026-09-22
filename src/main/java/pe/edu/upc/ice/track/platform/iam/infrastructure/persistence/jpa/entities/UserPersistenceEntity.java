package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA persistence entity for IAM users.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "password", nullable = false, length = 120)
  private String password;

  @Column(name = "email", unique = true, length = 120)
  private String email;

  /**
   * Identity provider the account authenticates with. Defaults to {@link AuthProvider#LOCAL}
   * so that rows created before federated sign-in was introduced keep a valid value.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "provider", length = 20)
  private AuthProvider provider = AuthProvider.LOCAL;

  /**
   * Identifier of the account at the external identity provider - the Google {@code sub} claim.
   * Null for local accounts.
   */
  @Column(name = "external_id", unique = true, length = 128)
  private String externalId;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<RolePersistenceEntity> roles = new HashSet<>();
}
