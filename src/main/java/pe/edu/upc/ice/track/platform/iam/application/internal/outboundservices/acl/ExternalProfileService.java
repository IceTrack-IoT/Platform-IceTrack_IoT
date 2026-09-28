package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the IAM bounded context provisions the profile of an account it
 * has just registered.
 *
 * <p>This is the <em>outbound half</em> of the Anti-Corruption Layer between {@code iam} and
 * {@code profiles}, and the only class in {@code iam} that knows a {@code profiles} context
 * exists. It talks to it exclusively through {@link ProfilesContextFacade}, the inbound facade,
 * whose whole signature is primitives and {@link String}s: no profiles aggregate, value object,
 * factory, repository, command or JPA entity is reachable from here.</p>
 *
 * <p>Keeping provisioning behind this service is what lets the {@code User} aggregate stay
 * focused on identity and authentication: it owns credentials, roles and the federated provider
 * link, and knows nothing about names, phone numbers or avatars.</p>
 *
 * <p>The call is an ordinary in-process invocation, so it participates in the caller's
 * transaction: a registration and its profile either both commit or both roll back.</p>
 */
@Service
@Slf4j
public class ExternalProfileService {

  /**
   * Sentinel the profiles facade returns when no profile could be found or created.
   */
  private static final long NO_PROFILE = 0L;

  private final ProfilesContextFacade profilesContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param profilesContextFacade the inbound ACL facade of the profiles bounded context
   */
  public ExternalProfileService(ProfilesContextFacade profilesContextFacade) {
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * Returns the identifier of the profile bound to an account, creating it when missing.
   *
   * <p>The operation is idempotent, so it can be invoked on every authentication - not only on
   * registration - without ever duplicating a profile.</p>
   *
   * @param userId   identifier of the freshly persisted account; required
   * @param fullName display name of the account holder; required
   * @param email    email address of the account holder; required
   * @param role     name of the role assigned to the account at registration
   * @return the profile identifier, or {@link Optional#empty()} when the profile could neither
   *         be found nor created
   */
  public Optional<Long> fetchOrCreateProfile(Long userId, String fullName, String email, String role) {
    return fetchOrCreateProfile(userId, fullName, email, null, role, null);
  }

  /**
   * Returns the identifier of the profile bound to an account, creating it when missing, and
   * passing along the optional details an identity provider may have released.
   *
   * @param userId        identifier of the freshly persisted account; required
   * @param fullName      display name of the account holder; required
   * @param email         email address of the account holder; required
   * @param phone         phone number of the account holder; may be {@code null}
   * @param role          name of the role assigned to the account at registration
   * @param auxiliaryData optional opaque annotation for the profile, such as the avatar URL
   *                      released by the identity provider; may be {@code null}
   * @return the profile identifier, or {@link Optional#empty()} when the profile could neither
   *         be found nor created
   */
  public Optional<Long> fetchOrCreateProfile(
      Long userId, String fullName, String email, String phone, String role, String auxiliaryData) {
    if (userId == null) {
      log.warn("Refusing to provision a profile without a user identifier");
      return Optional.empty();
    }

    // Fetch first so that a repeated sign-in never attempts a create. The facade is idempotent
    // as well, so this is a fast path rather than the only safeguard.
    var existingProfileId = profilesContextFacade.fetchProfileIdByUserId(userId);
    if (existingProfileId != null && existingProfileId != NO_PROFILE) {
      return Optional.of(existingProfileId);
    }

    var profileId = profilesContextFacade.createProfile(userId, fullName, email, phone, role, auxiliaryData);
    if (profileId == null || profileId == NO_PROFILE) {
      log.warn("The profiles context did not provision a profile for user {}", userId);
      return Optional.empty();
    }
    log.info("Provisioned profile {} for user {}", profileId, userId);
    return Optional.of(profileId);
  }
}
