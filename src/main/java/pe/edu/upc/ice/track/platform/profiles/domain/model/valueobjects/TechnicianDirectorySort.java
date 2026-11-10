package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * Order in which the public technician directory is answered.
 *
 * <p>Whatever the order, technicians that tie fall back to their name and then to their identifier,
 * so the order is total and a technician never appears on two pages nor on none.</p>
 */
public enum TechnicianDirectorySort {

  /** By full name, alphabetically. */
  NAME,

  /** By average rating, best first; technicians not rated yet come last. */
  RATING,

  /** By completed services, most first. */
  COMPLETED_SERVICES
}
