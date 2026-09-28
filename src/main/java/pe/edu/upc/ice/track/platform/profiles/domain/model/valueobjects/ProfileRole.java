package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * ProfileRole Value Object.
 *
 * <p>Domain specific role a profile plays inside the {@code profiles} bounded context. It is
 * intentionally declared here rather than reused from the {@code iam} context: the two contexts
 * evolve independently, and profiles must not depend on IAM types.</p>
 *
 * <p>There is deliberately no generic or provisional role: every persisted profile is either an
 * {@code OwnerProfile} or a {@code TechnicianProfile}, and the role is derived from that concrete
 * type rather than stored as free data on the aggregate.</p>
 */
public enum ProfileRole {

  /**
   * Owner of one or more ice tracks.
   */
  OWNER,

  /**
   * Technician in charge of maintaining ice tracks.
   */
  TECHNICIAN
}
