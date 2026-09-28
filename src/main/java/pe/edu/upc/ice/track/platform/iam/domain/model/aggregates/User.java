package pe.edu.upc.ice.track.platform.iam.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

/**
 * User aggregate root of the IAM bounded context.
 *
 * <p>Holds the credentials and the identity provider an account authenticates with. Accounts
 * federated through an external provider (currently {@link AuthProvider#GOOGLE}) carry no
 * password and are keyed by the provider's stable subject identifier, exposed here as
 * {@code externalId}.</p>
 *
 * <p><strong>Role invariant.</strong> An account holds exactly one role - {@link Roles#OWNER_ROLE}
 * or {@link Roles#TECHNICIAN_ROLE} - assigned when the aggregate is created and never changed
 * afterward: the field is {@code final}, there is no setter and no method that re-assigns it, and
 * {@link Role} itself is immutable. Switching roles would orphan the aggregates that depend on
 * the role in other bounded contexts, so it is impossible by construction rather than merely
 * forbidden.</p>
 */
@Getter
public class User extends AbstractDomainAggregateRoot<User> {

  /**
   * Placeholder stored in the password column for accounts federated through an external
   * identity provider, which never authenticate with a local password.
   */
  private static final String NO_LOCAL_PASSWORD = "";

  private final Long id;
  private final String username;
  private String password;
  private final String email;
  private final Role role;
  private AuthProvider provider;
  private String externalId;

  /**
   * Reconstitutes a user aggregate, typically from persistence.
   *
   * <p>New accounts must be created through {@link #registeredLocally} or
   * {@link #registeredWithGoogle}.</p>
   *
   * @param id         the persistence identity, or {@code null} for an account not yet persisted
   * @param username   the unique username; required
   * @param password   the encoded password; {@code null} is stored as the federated placeholder
   * @param email      the email address; required
   * @param role       the definitive role; required
   * @param provider   the identity provider; {@code null} defaults to {@link AuthProvider#LOCAL}
   * @param externalId the identifier at the external provider, {@code null} for local accounts
   * @throws IllegalArgumentException when a required value is missing
   */
  public User(Long id, String username, String password, String email, Role role, AuthProvider provider, String externalId) {
    this.id = id;
    this.username = requireText(username, "username");
    this.password = password == null ? NO_LOCAL_PASSWORD : password;
    this.email = requireText(email, "email");
    this.role = requireDefinitiveRole(role);
    this.provider = provider == null ? AuthProvider.LOCAL : provider;
    this.externalId = externalId;
  }

  /**
   * Registers a user that authenticates with a local username and password.
   *
   * @param username        the unique username; required
   * @param encodedPassword the already hashed password; required
   * @param email           the email address; required
   * @param role            the definitive role; required
   * @return the newly built user aggregate, not yet persisted
   * @throws IllegalArgumentException when a required value is missing
   */
  public static User registeredLocally(String username, String encodedPassword, String email, Role role) {
    requireText(encodedPassword, "password");
    return new User(null, username, encodedPassword, email, role, AuthProvider.LOCAL, null);
  }

  /**
   * Registers a user federated through Google.
   *
   * <p>The Google {@code sub} claim is stable for the lifetime of the account and is therefore
   * used as the external identifier; the email becomes the username so that the account is
   * addressable through the same unique key as a local one.</p>
   *
   * @param email     the verified email of the Google account; required
   * @param googleSub the Google {@code sub} claim identifying the account; required
   * @param role      the definitive role chosen during onboarding; required
   * @return the newly built user aggregate, not yet persisted
   * @throws IllegalArgumentException when a required value is missing
   */
  public static User registeredWithGoogle(String email, String googleSub, Role role) {
    return new User(null, email, NO_LOCAL_PASSWORD, email, role, AuthProvider.GOOGLE, requireText(googleSub, "googleSub"));
  }

  public void changePassword(String newPassword) {
    this.password = requireText(newPassword, "password");
  }

  /**
   * Links an already existing local account to a Google identity.
   *
   * <p>Used when a user who first registered locally signs in with a Google account carrying
   * the same email: the account keeps its identifier, credentials and role, and simply starts
   * accepting the federated provider as well.</p>
   *
   * @param googleSub the Google {@code sub} claim identifying the account; required
   */
  public void linkGoogleAccount(String googleSub) {
    this.externalId = requireText(googleSub, "googleSub");
    this.provider = AuthProvider.GOOGLE;
  }

  /**
   * Indicates whether this account authenticates through an external identity provider.
   *
   * @return {@code true} when the account is federated
   */
  public boolean isFederated() {
    return provider != null && provider != AuthProvider.LOCAL;
  }

  /**
   * Returns the name of the definitive role of this account, as used in the IAM published
   * language and in the issued bearer token.
   *
   * @return the role name, never {@code null}
   */
  public String getRoleName() {
    return role.getStringName();
  }

  /**
   * Indicates whether this account holds the given role.
   *
   * @param roleName the role to check
   * @return {@code true} when the account's definitive role is {@code roleName}
   */
  public boolean hasRole(Roles roleName) {
    return role.getName() == roleName;
  }

  private static Role requireDefinitiveRole(Role role) {
    if (role == null || role.getName() == null) {
      throw new IllegalArgumentException("A user must be created with exactly one role: OWNER_ROLE or TECHNICIAN_ROLE");
    }
    return role;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be null or blank".formatted(field));
    }
    return value;
  }
}
