package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;

/**
 * Domain contract for building a concrete {@link Profile}.
 *
 * <p>Every profile shares the attributes carried by {@link ProfileCreationData}, but each role
 * requires its own data - a taxpayer number for an owner, a qualification for a technician. The
 * factory is therefore typed by both the profile it builds and the role specific data it needs,
 * so a technician can never be built out of an owner's data, and no generic profile can be built
 * at all.</p>
 *
 * <p>Implementations are deliberately free of any framework annotation: they are pure domain
 * objects, instantiable and testable without a Spring context.</p>
 *
 * @param <P> the concrete profile type the factory builds
 * @param <A> the role specific data the factory requires
 */
public interface UserProfileFactory<P extends Profile, A> {

  /**
   * The role this factory produces profiles for.
   *
   * @return the supported role, never {@code null}
   */
  ProfileRole supportedRole();

  /**
   * Builds a profile from the supplied shared and role specific data.
   *
   * @param data             the validated shared creation data; required
   * @param roleSpecificData the validated data this role carries; required
   * @return the newly built, not yet persisted profile
   */
  P create(ProfileCreationData data, A roleSpecificData);
}
