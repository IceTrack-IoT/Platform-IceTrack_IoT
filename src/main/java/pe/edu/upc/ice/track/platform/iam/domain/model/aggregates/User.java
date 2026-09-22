package pe.edu.upc.ice.track.platform.iam.domain.model.aggregates;

import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * User aggregate root of the IAM bounded context.
 *
 * <p>Holds the credentials and the identity provider an account authenticates with. Accounts
 * federated through an external provider (currently {@link AuthProvider#GOOGLE}) carry no
 * password and are keyed by the provider's stable subject identifier, exposed here as
 * {@code externalId}.</p>
 */
@Getter
public class User extends AbstractDomainAggregateRoot<User> {

  /**
   * Placeholder stored in the password column for accounts federated through an external
   * identity provider, which never authenticate with a local password.
   */
  private static final String NO_LOCAL_PASSWORD = "";

  @Setter
  private Long id;
  @Setter
  private String username;
  @Setter
  private String password;
  @Setter
  private String email;
  @Setter
  private Set<Role> roles;
  @Setter
  private AuthProvider provider;
  @Setter
  private String externalId;

  public User() {
    this.roles = new HashSet<>();
    this.provider = AuthProvider.LOCAL;
  }

  public User(String username, String password) {
    this();
    this.username = username;
    this.password = password;
  }

  public User(String username, String password, List<Role> roles) {
    this();
    this.username = username;
    this.password = password;
    addRoles(roles);
  }

  public User(String username, String password, String email, List<Role> roles) {
    this(username, password, roles);
    this.email = email;
  }

  /**
   * Registers a user federated through Google.
   *
   * <p>The Google {@code sub} claim is stable for the lifetime of the account and is therefore
   * used as the external identifier; the email becomes the username so that the account is
   * addressable through the same unique key as a local one.</p>
   *
   * @param email      the verified email of the Google account; required
   * @param googleSub  the Google {@code sub} claim identifying the account; required
   * @param roles      the roles to assign; an empty or {@code null} list assigns the default role
   * @return the newly built user aggregate, not yet persisted
   */
  public static User registeredWithGoogle(String email, String googleSub, List<Role> roles) {
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(googleSub, "googleSub must not be null");
    var user = new User();
    user.username = email;
    user.email = email;
    user.password = NO_LOCAL_PASSWORD;
    user.provider = AuthProvider.GOOGLE;
    user.externalId = googleSub;
    user.addRoles(roles);
    return user;
  }

  /**
   * Add a role to the user.
   *
   * @param role the role to add
   * @return the user with the added role
   */
  public User assignRole(Role role) {
    this.roles.add(role);
    return this;
  }

  /**
   * Add a list of roles to the user.
   *
   * @param roles the list of roles to add
   */
  public void addRoles(List<Role> roles) {
    var validateRoleSet = Role.validateRoleSet(roles);
    this.roles.addAll(validateRoleSet);
  }

  public void changePassword(String newPassword) {
    this.password = newPassword;
  }

  /**
   * Links an already existing local account to a Google identity.
   *
   * <p>Used when a user who first registered locally signs in with a Google account carrying
   * the same email: the account keeps its identifier, credentials and roles, and simply starts
   * accepting the federated provider as well.</p>
   *
   * @param googleSub the Google {@code sub} claim identifying the account; required
   */
  public void linkGoogleAccount(String googleSub) {
    Objects.requireNonNull(googleSub, "googleSub must not be null");
    this.externalId = googleSub;
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
   * Returns the name of the primary role assigned to this user.
   *
   * <p>Used to describe the account in the IAM published language. Falls back to the default
   * role name when no role has been assigned yet.</p>
   *
   * @return the primary role name, never {@code null}
   */
  public String getPrimaryRoleName() {
    if (roles == null || roles.isEmpty()) {
      return Role.getDefaultRole().getStringName();
    }
    var defaultRoleName = Role.getDefaultRole().getStringName();
    var assignedRoleNames = roles.stream()
        .filter(role -> role != null && role.getName() != null)
        .map(Role::getStringName)
        .sorted()
        .toList();
    return assignedRoleNames.stream()
        .filter(roleName -> !roleName.equals(defaultRoleName))
        .findFirst()
        .orElse(defaultRoleName);
  }

}
