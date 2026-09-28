package pe.edu.upc.ice.track.platform.iam.domain.model.entities;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;

/**
 * Role domain entity representing a user role within the IceTrackPlatform application.
 *
 * <p>Immutable: a role handed out by a {@code User} can never be altered through it.</p>
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Role {
  private final Long id;
  private final Roles name;

  public Role(Roles name) {
    this(null, name);
  }

  /**
   * Get the name of the role as a string.
   *
   * @return the name of the role as a string
   */
  public String getStringName() {
    return name.name();
  }

  /***
   * Get the role from its name.
   *
   * @param name the name of the role, canonical ({@code OWNER_ROLE}) or bare ({@code OWNER})
   * @return the role corresponding to the given name
   * @throws IllegalArgumentException when the name matches no role
   */
  public static Role toRoleFromName(String name) {
    return new Role(Roles.fromName(name));
  }

}
