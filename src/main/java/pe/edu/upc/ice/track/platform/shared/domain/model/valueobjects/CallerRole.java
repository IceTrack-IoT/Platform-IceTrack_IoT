package pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects;

/**
 * The capacity in which an authenticated caller acts on this context.
 *
 * <p>Only the reads a technician needs in the field carry it - reading a site, the units of a site
 * or a single unit. Every write is owner-only and is always scoped by ownership.</p>
 */
public enum CallerRole {

  /** Reaches only the sites it owns and the units installed at them. */
  OWNER,

  /**
   * Services units of any owner, so its reads are scoped by its active service request
   * assignments rather than by ownership.
   */
  TECHNICIAN
}
