package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * EquipmentType enumerates the kind of refrigeration unit installed at a site.
 *
 * <p>The platform monitors positive-temperature refrigeration only (thresholds between 0 and
 * 30 °C, read by a DHT11 probe that cannot measure below 0 °C), so every type here holds chilled
 * goods above zero. A {@code DISPLAY_CASE} and a {@code REFRIGERATOR} hold chilled goods and share
 * a service manual, while a {@code COLD_ROOM} is a walk-in room installed and serviced
 * differently. {@code OTHER} exists so a unit that fits none of the three - a reach-in counter, a
 * wine cooler - can still be catalogued instead of being mislabeled as one of them.</p>
 */
public enum EquipmentType {

  /** Open refrigerated display counter holding chilled goods above 0 degrees Celsius. */
  DISPLAY_CASE,

  /** Walk-in chilled storage room, usually serving an entire premises. */
  COLD_ROOM,

  /** Closed domestic or commercial refrigerator holding chilled goods above 0 degrees Celsius. */
  REFRIGERATOR,

  /** Any positive-temperature refrigeration unit that fits none of the above. */
  OTHER
}
