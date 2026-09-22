package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import java.util.Locale;

/**
 * ProfileRole Value Object.
 *
 * <p>Domain specific role a profile plays inside the {@code profiles} bounded context. It is
 * intentionally declared here rather than reused from the {@code iam} context: the two contexts
 * evolve independently, and profiles must not depend on IAM types. Role names travelling in the
 * IAM published language are translated through {@link #fromRoleName(String)}.</p>
 */
public enum ProfileRole {

  /**
   * Default role of a profile whose owner has no specialised responsibility yet.
   */
  USER,

  /**
   * Owner of one or more ice tracks.
   */
  OWNER,

  /**
   * Technician in charge of maintaining ice tracks.
   */
  TECHNICIAN;

  private static final String ROLE_SUFFIX = "_ROLE";

  /**
   * Translates a role name coming from another bounded context into a profile role.
   *
   * <p>Accepts both the IAM naming convention ({@code OWNER_ROLE}) and the bare profile naming
   * ({@code OWNER}), in any case. Unknown, blank or {@code null} names resolve to {@link #USER},
   * so that an unexpected value never prevents a profile from being created.</p>
   *
   * @param roleName the role name to translate
   * @return the matching profile role, never {@code null}
   */
  public static ProfileRole fromRoleName(String roleName) {
    if (roleName == null || roleName.isBlank()) {
      return USER;
    }
    var normalized = roleName.trim().toUpperCase(Locale.ROOT);
    if (normalized.endsWith(ROLE_SUFFIX)) {
      normalized = normalized.substring(0, normalized.length() - ROLE_SUFFIX.length());
    }
    for (var role : values()) {
      if (role.name().equals(normalized)) {
        return role;
      }
    }
    return USER;
  }
}
