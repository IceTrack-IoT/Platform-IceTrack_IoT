package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * Identifier of a service request.
 *
 * @param serviceRequestId the persistence identity; required and strictly positive
 */
public record ServiceRequestId(Long serviceRequestId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public ServiceRequestId {
    if (serviceRequestId == null || serviceRequestId <= 0) {
      throw new IllegalArgumentException("Service request identifier must be a positive number");
    }
  }
}
