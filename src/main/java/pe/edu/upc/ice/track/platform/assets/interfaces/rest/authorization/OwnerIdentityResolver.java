package pe.edu.upc.ice.track.platform.assets.interfaces.rest.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.ExternalOwnerService;

import java.util.Optional;

/**
 * Resolves the owner a request is acting as.
 *
 * <p>Sites and equipment in this context belong to an <em>owner profile</em>, but what a request
 * actually carries is a bearer token whose principal is a username. This component bridges the
 * two through {@link ExternalOwnerService}, which walks username to platform account to owner
 * profile, and hands the resulting identifier to the command and query services, which apply the
 * ownership rules.</p>
 *
 * <p>Nothing here authenticates: {@code WebSecurityConfiguration} has already rejected an
 * unauthenticated request with a 401 before any controller runs. This component only decides
 * <em>which</em> owner an authenticated caller is, and for that it reads the Spring Security
 * principal exclusively. No request payload, path variable or query parameter can influence the
 * answer, so a client cannot act as an owner it does not hold - which is the whole point of
 * scoping every query and command of this context by {@code ownerId} rather than trusting a
 * caller-supplied one.</p>
 *
 * <p>An authenticated account that owns no owner profile - a technician, say - resolves to
 * {@link Optional#empty()}; the controllers report that as 403, since the caller is known and
 * merely has nothing here.</p>
 */
@Component("ownerIdentityResolver")
public class OwnerIdentityResolver {

  private final ExternalOwnerService externalOwnerService;

  /**
   * Constructor
   *
   * @param externalOwnerService The {@link ExternalOwnerService} instance
   */
  public OwnerIdentityResolver(ExternalOwnerService externalOwnerService) {
    this.externalOwnerService = externalOwnerService;
  }

  /**
   * Resolves the owner identifier of the authenticated caller.
   *
   * @param authentication The {@link Authentication} of the current request, may be {@code null}
   * @return The owner identifier, or {@link Optional#empty()} when the caller holds no owner profile
   */
  public Optional<Long> resolveOwnerId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    return externalOwnerService.fetchOwnerIdByUsername(authentication.getName());
  }

  /**
   * Resolves the owner identifier of the authenticated caller, or {@code null} when there is none.
   *
   * <p>Convenience for the controllers, whose endpoints are all shaped as "resolve, then answer
   * 403 when nothing resolved".</p>
   *
   * @param authentication The {@link Authentication} of the current request, may be {@code null}
   * @return The owner identifier, or {@code null}
   */
  public Long resolveOwnerIdOrNull(Authentication authentication) {
    return resolveOwnerId(authentication).orElse(null);
  }
}