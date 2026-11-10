package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * Identifier of the owner profile a service request belongs to.
 *
 * <p>The owner lives in the {@code profiles} context; this context only holds its identifier.</p>
 *
 * @param ownerId the owner profile identifier; required and strictly positive
 */
public record OwnerId(Long ownerId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public OwnerId {
    if (ownerId == null || ownerId <= 0) {
      throw new IllegalArgumentException("Owner identifier must be a positive number");
    }
  }
}
