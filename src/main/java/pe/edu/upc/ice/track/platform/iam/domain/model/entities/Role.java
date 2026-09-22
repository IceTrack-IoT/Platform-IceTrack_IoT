package pe.edu.upc.ice.track.platform.iam.domain.model.entities;


import lombok.*;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;

import java.util.List;

/**
 * Role domain entity representing a user role within the IceTrackPlatform application.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@With
@EqualsAndHashCode
@ToString
public class Role {
  private Long id;
  private Roles name;

  public Role(Roles name) {
    this.name = name;
  }

  /**
   * Get the name of the role as a string.
   *
   * @return the name of the role as a string
   */
  public String getStringName() {
    return name.name();
  }

  /**
   * Get the default role for a new user.
   *
   * @return the default role
   */
  public static Role getDefaultRole() {
    return new Role(Roles.USER_ROLE);
  }

  /***
   * Get the role from its name.
   *
   * @param name the name of the role
   * @return the role corresponding to the given name
   */
  public static Role toRoleFromName(String name) {
    return new Role(Roles.valueOf(name));
  }

  /**
   * Validate the role set.
   *
   * @param roles the list of roles to validate
   * @return the validated list of roles
   */
  public static List<Role> validateRoleSet(List<Role> roles) {
    if (roles == null || roles.isEmpty()) {
      return List.of(getDefaultRole());
    } else {
      return roles;
    }
  }

}
