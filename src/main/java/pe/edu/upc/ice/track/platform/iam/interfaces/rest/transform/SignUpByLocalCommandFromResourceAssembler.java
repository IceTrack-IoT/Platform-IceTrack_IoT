package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpWithLocalResource;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Assembler that converts a {@link SignUpWithLocalResource} into a {@link SignUpByLocalCommand}.
 */
public class SignUpByLocalCommandFromResourceAssembler {

  /**
   * Converts the local sign-up payload into its command representation.
   *
   * <p>Unknown or blank role names are dropped; an empty role list makes the command service
   * fall back to the default role.</p>
   *
   * @param resource the {@link SignUpWithLocalResource} resource to convert
   * @return the {@link SignUpByLocalCommand} command
   */
  public static SignUpByLocalCommand toCommandFromResource(SignUpWithLocalResource resource) {
    var roles = resource.roles() == null
        ? List.<Role>of()
        : resource.roles().stream()
            .filter(roleName -> roleName != null && !roleName.isBlank())
            .map(roleName -> toRoleOrNull(roleName.trim().toUpperCase(Locale.ROOT)))
            .filter(Objects::nonNull)
            .toList();
    return new SignUpByLocalCommand(resource.username(), resource.password(), resource.email(), roles);
  }

  /**
   * Resolves a role name, ignoring values that do not match a known role.
   *
   * @param roleName the submitted role name, already normalised to upper case
   * @return the matching {@link Role}, or {@code null} when the name is unknown
   */
  private static Role toRoleOrNull(String roleName) {
    try {
      return Role.toRoleFromName(roleName);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
