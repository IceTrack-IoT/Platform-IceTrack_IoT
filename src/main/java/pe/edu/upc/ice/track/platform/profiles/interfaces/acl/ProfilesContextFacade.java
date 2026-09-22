package pe.edu.upc.ice.track.platform.profiles.interfaces.acl;

/**
 * ACL facade that exposes Profiles bounded context capabilities to other contexts.
 *
 * <p>This is the <em>inbound port</em> of the Anti-Corruption Layer around the {@code profiles}
 * context. Every parameter is a primitive or a {@link String}: no profiles aggregate, value
 * object, command, repository or JPA entity appears in this signature, and no foreign type is
 * accepted either. The implementation is solely responsible for translating these agnostic
 * values into the profiles domain model, so a caller can never contaminate it.</p>
 *
 * <p>Callers depend on this interface only. They must not reach into
 * {@code profiles.domain.*}, {@code profiles.application.*} or
 * {@code profiles.infrastructure.*}.</p>
 */
public interface ProfilesContextFacade {

  /**
   * Creates the profile of a platform account, or returns the existing one.
   *
   * <p>The operation is idempotent: when a profile is already linked to {@code userId} - or
   * already registered under {@code email} - its identifier is returned and nothing is created.
   * This is what lets an identity context call it on every sign-in without duplicating data.</p>
   *
   * @param userId        identifier of the account the profile belongs to; required
   * @param fullName      display name of the account holder; required. It is split into a given
   *                      and a family name by the profiles context
   * @param email         email address of the account holder; required, and must be well formed
   * @param phone         phone number of the account holder; may be {@code null}
   * @param role          role the profile plays, accepted either as a bare profile role
   *                      ({@code OWNER}) or with the calling context's suffix
   *                      ({@code OWNER_ROLE}); an unknown value falls back to the default role
   * @param auxiliaryData optional opaque annotation stored verbatim on the profile, such as the
   *                      avatar URL released by an identity provider; may be {@code null}
   * @return the profile identifier, or {@code 0L} when the profile could neither be found nor
   *         created because the supplied values violate a profiles domain constraint
   */
  Long createProfile(Long userId, String fullName, String email, String phone, String role, String auxiliaryData);

  /**
   * Fetches the identifier of the profile linked to a platform account.
   *
   * @param userId identifier of the account
   * @return profile identifier, or {@code 0L} when not found
   */
  Long fetchProfileIdByUserId(Long userId);

  /**
   * Fetches a profile identifier by email.
   *
   * @param email profile email address
   * @return profile identifier, or {@code 0L} when not found or when the email is malformed
   */
  Long fetchProfileIdByEmail(String email);
}
