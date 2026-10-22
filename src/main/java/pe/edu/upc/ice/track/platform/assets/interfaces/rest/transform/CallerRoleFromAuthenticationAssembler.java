package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;

/**
 * Assembler that converts the authorities of an {@link Authentication} into a {@link CallerRole}.
 */
public class CallerRoleFromAuthenticationAssembler {

  private static final String TECHNICIAN_AUTHORITY = "TECHNICIAN_ROLE";

  /**
   * Converts the authorities of the authenticated principal into the role it acts in.
   *
   * <p>Only an explicit {@code TECHNICIAN_ROLE} authority yields {@link CallerRole#TECHNICIAN}.
   * Anything else falls back to {@link CallerRole#OWNER}, whose operations are always scoped by
   * ownership, so a missing or unexpected authority can never widen what the caller reaches.</p>
   *
   * @param authentication the authenticated principal
   * @return the {@link CallerRole} the command and query services scope by
   */
  public static CallerRole toCallerRoleFromAuthentication(Authentication authentication) {
    var isTechnician = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(TECHNICIAN_AUTHORITY::equals);
    return isTechnician ? CallerRole.TECHNICIAN : CallerRole.OWNER;
  }
}
