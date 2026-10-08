package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST representation of the operational status of a unit.
 *
 * <p>A separate type from the domain {@code StatusEquipment}, for the same reason
 * {@link EquipmentTypeResource} is a separate type from its domain counterpart: an unrecognised
 * value must be rejected at the boundary with a 400 rather than surfacing as a deserialisation
 * failure.</p>
 */
@Schema(
    name = "StatusEquipment",
    description = "Operational status of a refrigeration unit",
    example = "ON")
public enum StatusEquipmentResource {

  AVAILABLE,
  ON,
  OFF,
  OFFLINE;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain status
   */
  public pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment toDomain() {
    return pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment.valueOf(name());
  }

  /**
   * Translates a domain status into this REST constant.
   *
   * @param status the domain status; required
   * @return the matching REST constant
   */
  public static StatusEquipmentResource fromDomain(
      pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment status) {
    return valueOf(status.name());
  }
}