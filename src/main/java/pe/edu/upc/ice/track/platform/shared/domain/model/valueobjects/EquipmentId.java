package pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects;

/**
 * A value object representing an equipment unit's unique identifier in the shared kernel.
 *
 * <p>Every bounded context that only holds a reference to an equipment - rather than the aggregate
 * itself - uses this type. It keeps the identifier immutable and rejects the values that can never
 * be a persisted identity: {@code null} and anything that is not strictly positive.</p>
 *
 * @param equipmentId the equipment identifier; required and strictly positive
 */
public record EquipmentId(Long equipmentId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public EquipmentId {
    if (equipmentId == null || equipmentId <= 0) {
      throw new IllegalArgumentException("Equipment identifier must be a positive number");
    }
  }
}
