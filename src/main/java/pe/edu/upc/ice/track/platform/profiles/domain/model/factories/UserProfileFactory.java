package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;

/**
 * Domain contract for building a {@link Profile} that matches a given {@link ProfileRole}.
 *
 * <p>Profile instantiation is role driven: an owner and a technician are both profiles, but they
 * are born with different responsibilities inside the {@code profiles} bounded context. Keeping
 * the decision behind this contract means callers - in particular the listener reacting to an
 * IAM registration - never branch on a role themselves; they resolve the factory through
 * {@link #forRole(ProfileRole)} and delegate.</p>
 *
 * <p>Implementations are deliberately free of any framework annotation: they are pure domain
 * objects, instantiable and testable without a Spring context.</p>
 */
public interface UserProfileFactory {

  /**
   * The role this factory produces profiles for.
   *
   * @return the supported role, never {@code null}
   */
  ProfileRole supportedRole();

  /**
   * Builds a profile from the supplied creation data.
   *
   * @param data the validated creation data
   * @return the newly built, not yet persisted profile
   */
  Profile createFrom(ProfileCreationData data);

  /**
   * Resolves the factory responsible for a role.
   *
   * @param role the role to build a profile for; {@code null} resolves to {@link ProfileRole#USER}
   * @return the matching factory, never {@code null}
   */
  static UserProfileFactory forRole(ProfileRole role) {
    var resolvedRole = role == null ? ProfileRole.USER : role;
    return switch (resolvedRole) {
      case OWNER -> new OwnerProfileFactory();
      case TECHNICIAN -> new TechnicianProfileFactory();
      case USER -> new StandardProfileFactory();
    };
  }
}
