package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;

/**
 * REST representation of the lifecycle status of a service request.
 *
 * <p>A separate type from the domain {@link ServiceStatus}, as the {@code assets} resources do, so
 * the REST contract does not move whenever the domain enum does. Values are matched exactly, in
 * upper case, both as a query parameter and in a JSON body.</p>
 */
@Schema(
    name = "ServiceStatus",
    description = "Lifecycle status of a service request",
    example = "PENDING")
public enum ServiceStatusResource {

  PENDING,
  ACCEPTED,
  IN_PROGRESS,
  COMPLETED,
  CANCELED;

  /**
   * Translates this REST constant into its domain counterpart.
   *
   * @return the domain status
   */
  public ServiceStatus toDomain() {
    return ServiceStatus.valueOf(name());
  }

  /**
   * Translates a domain status into this REST constant.
   *
   * @param status the domain status, may be {@code null}
   * @return the matching REST constant, or {@code null} when there is no status
   */
  public static ServiceStatusResource fromDomain(ServiceStatus status) {
    return status == null ? null : valueOf(status.name());
  }
}
