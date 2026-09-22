package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.profiles;

import java.util.Optional;

/**
 * Outbound service (port) through which the IAM bounded context provisions the profile of an
 * account it has just registered.
 *
 * <p>This is the <em>outbound half</em> of the Anti-Corruption Layer between {@code iam} and
 * {@code profiles}. The IAM application layer depends on this port and on nothing else: it never
 * sees a profiles aggregate, factory, repository or command, and it never learns how - or even
 * whether - a profile is actually stored. The adapter that implements it is the only class in
 * {@code iam} allowed to know that a {@code profiles} context exists.</p>
 *
 * <p>Keeping provisioning behind this port is what lets the {@code User} aggregate stay focused
 * on identity and authentication: it owns credentials, roles and the federated provider link,
 * and knows nothing about names, phone numbers or avatars.</p>
 */
public interface ExternalProfileService {

  /**
   * Returns the identifier of the profile bound to an account, creating it when missing.
   *
   * <p>Implementations must be idempotent, so that the operation can be invoked on every
   * authentication - not only on registration - without ever duplicating a profile.</p>
   *
   * @param userId   identifier of the freshly persisted account; required
   * @param fullName display name of the account holder; required
   * @param email    email address of the account holder; required
   * @param role     name of the role assigned to the account at registration
   * @return the profile identifier, or {@link Optional#empty()} when the profile could neither
   *         be found nor created
   */
  Optional<Long> fetchOrCreateProfile(Long userId, String fullName, String email, String role);

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
  Optional<Long> fetchOrCreateProfile(
      Long userId, String fullName, String email, String phone, String role, String auxiliaryData);
}
