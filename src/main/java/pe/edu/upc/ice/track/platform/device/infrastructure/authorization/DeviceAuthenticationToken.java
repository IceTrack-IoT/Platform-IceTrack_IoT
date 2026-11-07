package pe.edu.upc.ice.track.platform.device.infrastructure.authorization;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;

import java.util.List;
import java.util.Objects;

/**
 * Authentication of a paired IoT board, placed in the security context by
 * {@link ApiKeyAuthenticationFilter}.
 *
 * <p>Always authenticated and carrying the single {@link RoleAuthorities#ROLE_DEVICE} authority.
 * Its principal is an {@link AuthenticatedDevice}, which controllers receive through
 * {@code @AuthenticationPrincipal}. It carries no credentials: the API key is dropped as soon as
 * it has been verified, so it can never leak through the security context.</p>
 */
public class DeviceAuthenticationToken extends AbstractAuthenticationToken {

  private final AuthenticatedDevice principal;

  public DeviceAuthenticationToken(AuthenticatedDevice principal) {
    super(List.of(new SimpleGrantedAuthority(RoleAuthorities.ROLE_DEVICE)));
    this.principal = Objects.requireNonNull(principal, "principal must not be null");
    super.setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public AuthenticatedDevice getPrincipal() {
    return principal;
  }

  @Override
  public String getName() {
    return "device:" + principal.deviceId();
  }

  /** Rejects any attempt to mark the token trusted or untrusted after construction. */
  @Override
  public void setAuthenticated(boolean authenticated) {
    throw new IllegalArgumentException("A device authentication token is always authenticated");
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof DeviceAuthenticationToken token
        && super.equals(token) && principal.equals(token.principal);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + principal.hashCode();
  }
}
