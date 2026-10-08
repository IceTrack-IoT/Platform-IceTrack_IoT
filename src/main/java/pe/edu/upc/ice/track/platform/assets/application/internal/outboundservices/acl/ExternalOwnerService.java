package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the {@code assets} bounded context resolves <em>who the caller
 * is</em> before acting on its behalf.
 *
 * <p>This is the outbound half of two Anti-Corruption Layers chained, and the only class in
 * {@code assets} that knows IAM or Profiles exist. It answers a single question, so it keeps the
 * chain inside: sites and equipment are owned by an <em>owner profile</em>, an owner profile
 * belongs to a <em>platform account</em>, and the only thing a request carries is a bearer token
 * whose principal is a username. Resolving that username to an account through
 * {@link IamContextFacade} and then that account to an owner through
 * {@link ProfilesContextFacade} is what turns "an authenticated caller" into "this owner", which
 * is the value every command and query in this context is scoped by.</p>
 *
 * <p>Both hops are read-only lookups against published facades made of primitives and strings, so
 * neither an IAM nor a Profiles aggregate, entity, repository or security type is reachable from
 * here - the same discipline {@code profiles} follows towards IAM in its own
 * {@code ExternalIamService}.</p>
 *
 * <p>A caller that authenticates but owns no owner profile yields {@link Optional#empty()} rather
 * than an exception. That is a legitimate state - a technician account hitting an owner-only
 * endpoint - and it is the caller's job to decide which status code reports it.</p>
 */
@Service
public class ExternalOwnerService {

  /**
   * Sentinel the IAM facade returns when no account matches a username.
   */
  private static final long NO_USER = 0L;

  /**
   * Sentinel the profiles facade returns when no owner profile is bound to an account.
   */
  private static final long NO_OWNER = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param iamContextFacade      the inbound ACL facade of the IAM bounded context
   * @param profilesContextFacade the inbound ACL facade of the profiles bounded context
   */
  public ExternalOwnerService(
      IamContextFacade iamContextFacade,
      ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * Resolves the owner profile a platform account acts as.
   *
   * @param userId identifier of the platform account
   * @return the owner profile identifier, or empty when the account holds no owner profile
   */
  public Optional<Long> fetchOwnerIdByUserId(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }
    var ownerId = profilesContextFacade.fetchOwnerIdByUserId(userId);
    return ownerId == null || ownerId == NO_OWNER ? Optional.empty() : Optional.of(ownerId);
  }

  /**
   * Resolves the owner profile a username acts as.
   *
   * @param username username of the platform account, may be {@code null} or blank
   * @return the owner profile identifier, or empty when no account holds that username or the
   *         account owns no owner profile
   */
  public Optional<Long> fetchOwnerIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    if (userId == null || userId == NO_USER) {
      return Optional.empty();
    }
    return fetchOwnerIdByUserId(userId);
  }
}