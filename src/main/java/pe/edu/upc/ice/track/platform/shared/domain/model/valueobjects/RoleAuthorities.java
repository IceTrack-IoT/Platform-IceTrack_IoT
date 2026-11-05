package pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects;

/**
 * The authority names the platform attaches to an authenticated account.
 *
 * <p>Keeping the literals in one place stops a controller annotation and the token-issuing code
 * from drifting apart: a typo in either would silently turn an owner-only endpoint into one that
 * denies everybody, or one that accepts a caller it should not. Security expressions reference
 * these constants directly because they are compile-time constants, so an annotation such as
 * {@code @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")} still works.</p>
 */
public final class RoleAuthorities {

  private RoleAuthorities() {
  }

  /** Authority granted to an account that owns sites and equipment. */
  public static final String ROLE_OWNER = "OWNER_ROLE";

  /** Authority granted to an account that services equipment in the field. */
  public static final String ROLE_TECHNICIAN = "TECHNICIAN_ROLE";
}
