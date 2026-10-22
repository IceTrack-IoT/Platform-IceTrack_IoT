package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * The capacity in which an authenticated caller acts on this context.
 *
 * <p>Only the operations a technician needs in the field carry it - reading a site, the units of a
 * site or a single unit, and changing a unit's status. Every other operation is owner-only and is
 * always scoped by ownership.</p>
 */
public enum CallerRole {

  /** Reaches only the sites it owns and the units installed at them. */
  OWNER,

  /** Services units of any owner, so the operations open to it are not scoped by ownership. */
  TECHNICIAN
}
