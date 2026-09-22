package pe.edu.upc.ice.track.platform.iam.domain.repositories;

import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;

import java.util.List;
import java.util.Optional;

/**
 * Role repository
 * <p>
 *     This interface represents the repository for the Role entity.
 * </p>
 */
public interface RoleRepository {
  /**
   * Find role by name
   *
   * @param name the name of the role
   * @return an optional containing the role if found, or empty if not found
   */
  Optional<Role> findByName(Roles name);

  /**
   * Find all roles
   *
   * @return a list of all roles
   */
  List<Role> findAll();

  /**
   * Save role
   *
   * @param role the role to save
   * @return the saved role
   */
  Role save(Role role);

  /**
   * Check if role exists by name
   *
   * @param name the name of the role
   * @return true if the role exists, false otherwise
   */
  boolean existsByName(Roles name);
}
