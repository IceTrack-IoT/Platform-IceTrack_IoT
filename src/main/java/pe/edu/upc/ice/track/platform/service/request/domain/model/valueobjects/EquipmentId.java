package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * Identifier of the refrigeration unit a service request is about.
 *
 * <p>The unit lives in the {@code assets} context; this context only holds its identifier.</p>
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
