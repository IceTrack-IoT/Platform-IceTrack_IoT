package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * Identifier of the technician profile a service request is assigned to.
 *
 * <p>The technician lives in the {@code profiles} context; this context only holds its
 * identifier.</p>
 *
 * @param technicianProfileId the technician profile identifier; required and strictly positive
 */
public record TechnicianProfileId(Long technicianProfileId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public TechnicianProfileId {
    if (technicianProfileId == null || technicianProfileId <= 0) {
      throw new IllegalArgumentException("Technician profile identifier must be a positive number");
    }
  }
}
