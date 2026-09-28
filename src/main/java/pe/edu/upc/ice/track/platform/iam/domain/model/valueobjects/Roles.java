package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

import java.util.Locale;

/**
 * Enumeration of user roles within the IceTrackPlatform application.
 *
 * <p>An account holds exactly one of these roles, chosen at registration and never changed
 * afterward. There is deliberately no default or provisional role.</p>
 */
public enum Roles {
  OWNER_ROLE,
  TECHNICIAN_ROLE;

  private static final String ROLE_SUFFIX = "_ROLE";

  /**
   * Resolves a role from its name.
   *
   * <p>Accepts both the canonical name ({@code OWNER_ROLE}) and its bare form ({@code OWNER}), in
   * any case.</p>
   *
   * @param name the role name
   * @return the matching role, never {@code null}
   * @throws IllegalArgumentException when the name is blank or matches no role
   */
  public static Roles fromName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Role must not be null or blank");
    }
    var normalized = name.trim().toUpperCase(Locale.ROOT);
    if (!normalized.endsWith(ROLE_SUFFIX)) {
      normalized = normalized + ROLE_SUFFIX;
    }
    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Unknown role '%s'; expected OWNER_ROLE or TECHNICIAN_ROLE".formatted(name));
    }
  }
}
