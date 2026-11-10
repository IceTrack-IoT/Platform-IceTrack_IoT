package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization;

/**
 * How much of a profile an authenticated caller may see.
 *
 * <p>Decided by {@link ProfileAccessEvaluator} and applied by the profile resource assemblers, so
 * the response shape never changes: fields the caller may not see are sent as {@code null}.</p>
 */
public enum ProfileView {

  /** The caller's own profile: every field. */
  FULL,

  /**
   * An owner and a technician working together on an active service request: name and contact
   * channels (email, phone), but no home or business address and no tax identifier.
   */
  CONTACT,

  /** An owner browsing the technician directory: name and professional data only. */
  PUBLIC,

  /** Nothing: the profile is answered as not found. */
  NONE
}
