package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;

/**
 * REST representation of the kind of work a service request asks for.
 */
@Schema(
    name = "ServiceType",
    description = "Kind of work a service request asks for",
    example = "REPAIR")
public enum ServiceTypeResource {

  PREVENTIVE_MAINTENANCE,
  REPAIR;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain type
   */
  public ServiceType toDomain() {
    return ServiceType.valueOf(name());
  }

  /**
   * Translates a domain type into this REST constant.
   *
   * @param type the domain type; required
   * @return the matching REST constant
   */
  public static ServiceTypeResource fromDomain(ServiceType type) {
    return valueOf(type.name());
  }
}
