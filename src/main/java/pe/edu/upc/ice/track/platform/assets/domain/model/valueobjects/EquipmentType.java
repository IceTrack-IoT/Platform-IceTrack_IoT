package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * EquipmentType enumerates the kind of refrigeration unit installed at a site.
 *
 * <p>The values are the ones the ubiquitous language of the domain actually uses on the floor of a
 * heladeria, supermercado or laboratorio: a {@code FREEZER} and a {@code COLD_ROOM} both run well
 * below zero but are installed and serviced differently, whereas a {@code DISPLAY_CASE} and a
 * {@code REFRIGERATOR} hold chilled goods above zero and share a service manual. {@code OTHER}
 * exists so a unit that fits none of the four - an ultra-low temperature freezer, a reach-in
 * counter - can still be catalogued instead of being mislabelled as one of them.</p>
 */
public enum EquipmentType {

  /** Horizontal or vertical chest/standing freezer running well below 0 degrees Celsius. */
  FREEZER,

  /** Open refrigerated display counter holding chilled goods above 0 degrees Celsius. */
  DISPLAY_CASE,

  /** Walk-in cold storage room, usually serving an entire premises. */
  COLD_ROOM,

  /** Closed domestic or commercial refrigerator holding chilled goods above 0 degrees Celsius. */
  REFRIGERATOR,

  /** Any refrigeration unit that fits none of the above. */
  OTHER
}