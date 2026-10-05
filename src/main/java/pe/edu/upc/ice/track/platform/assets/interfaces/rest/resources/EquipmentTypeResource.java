package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST representation of the kind of refrigeration unit.
 *
 * <p>A separate type from the domain {@code EquipmentType} on purpose. Binding a domain enum
 * directly would let an unrecognised value fail as a deserialisation error deep inside Jackson,
 * which this platform reports as a 500; going through this enum means the value is turned into a
 * 400 "unknown value" at the boundary, and it gives the published API a vocabulary of its own that
 * can gain or lose a constant without silently changing how stored rows are read.</p>
 */
@Schema(
    name = "EquipmentType",
    description = "Kind of refrigeration unit",
    example = "FREEZER")
public enum EquipmentTypeResource {

  FREEZER,
  DISPLAY_CASE,
  COLD_ROOM,
  REFRIGERATOR,
  OTHER;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain equipment type
   */
  public pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType toDomain() {
    return pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType.valueOf(name());
  }

  /**
   * Translates a domain equipment type into this REST constant.
   *
   * @param type the domain equipment type; required
   * @return the matching REST constant
   */
  public static EquipmentTypeResource fromDomain(
      pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType type) {
    return valueOf(type.name());
  }
}