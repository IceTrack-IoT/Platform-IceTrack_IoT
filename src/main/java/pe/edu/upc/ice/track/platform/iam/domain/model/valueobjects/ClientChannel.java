package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;

/**
 * The client application a session is opened from.
 *
 * <p>Each role has exactly one application: owners use the web application and technicians use
 * the mobile application. Every sign-in states its channel, and a session is only opened when the
 * account's role is the one the channel admits. The channel a session was opened from is stored
 * with its refresh token, so a refresh can never move a session to another application.</p>
 */
public enum ClientChannel {

  /** The web application, reserved for {@link Roles#OWNER_ROLE}. */
  WEB(Roles.OWNER_ROLE),

  /** The mobile application, reserved for {@link Roles#TECHNICIAN_ROLE}. */
  MOBILE(Roles.TECHNICIAN_ROLE);

  private final Roles admittedRole;

  ClientChannel(Roles admittedRole) {
    this.admittedRole = admittedRole;
  }

  /**
   * Returns the only role allowed to open a session through this channel.
   *
   * @return the admitted role, never {@code null}
   */
  public Roles getAdmittedRole() {
    return admittedRole;
  }

  /**
   * Tells whether an account may open a session through this channel.
   *
   * @param user the authenticated account
   * @return {@code true} when the account holds the role this channel admits
   */
  public boolean admits(User user) {
    return user != null && user.hasRole(admittedRole);
  }

  /**
   * Returns the channel reserved for a role.
   *
   * @param role the role of an account; required
   * @return the channel admitting {@code role}
   * @throws IllegalArgumentException when no channel admits the role
   */
  public static ClientChannel forRole(Roles role) {
    for (var channel : values()) {
      if (channel.admittedRole == role) {
        return channel;
      }
    }
    throw new IllegalArgumentException("No client channel admits role " + role);
  }

  /**
   * Returns the channel an account must use.
   *
   * @param user the account; required
   * @return the channel admitting the account's role
   * @throws IllegalArgumentException when no channel admits the account's role
   */
  public static ClientChannel requiredFor(User user) {
    for (var channel : values()) {
      if (channel.admits(user)) {
        return channel;
      }
    }
    throw new IllegalArgumentException("No client channel admits role " + user.getRoleName());
  }
}
