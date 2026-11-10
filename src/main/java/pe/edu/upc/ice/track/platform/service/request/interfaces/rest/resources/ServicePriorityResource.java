package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;

/**
 * REST representation of the urgency of a service request.
 */
@Schema(
    name = "ServicePriority",
    description = "Urgency of a service request",
    example = "HIGH")
public enum ServicePriorityResource {

  LOW,
  MEDIUM,
  HIGH,
  URGENT;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain priority
   */
  public ServicePriority toDomain() {
    return ServicePriority.valueOf(name());
  }

  /**
   * Translates a domain priority into this REST constant.
   *
   * @param priority the domain priority; required
   * @return the matching REST constant
   */
  public static ServicePriorityResource fromDomain(ServicePriority priority) {
    return valueOf(priority.name());
  }
}
