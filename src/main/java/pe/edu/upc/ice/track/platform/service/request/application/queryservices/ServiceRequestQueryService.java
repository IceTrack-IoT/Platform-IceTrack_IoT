package pe.edu.upc.ice.track.platform.service.request.application.queryservices;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestByIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListEquipmentInterventionsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListServiceRequestsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;

import java.util.List;
import java.util.Optional;

/**
 * Application service handling read operations on {@link ServiceRequest}.
 *
 * <p>Every read is scoped to its caller. Something out of the caller's reach is reported as empty,
 * exactly like something that does not exist, so a read cannot be used to discover which
 * identifiers are real.</p>
 */
public interface ServiceRequestQueryService {

  /**
   * Fetch a single request with its status history.
   *
   * @param query The {@link GetServiceRequestByIdQuery} Query
   * @return the request, or empty when it does not exist or the caller is neither its owner nor its
   *         assigned technician
   */
  Optional<ServiceRequest> handle(GetServiceRequestByIdQuery query);

  /**
   * List one page of the caller's requests, newest first.
   *
   * @param query The {@link ListServiceRequestsQuery} Query
   * @return the requested page, empty when the caller holds no profile of its role
   */
  ServiceRequestPage handle(ListServiceRequestsQuery query);

  /**
   * List the completed requests of a unit, each holding the intervention that completed it.
   *
   * @param query The {@link ListEquipmentInterventionsQuery} Query
   * @return the completed requests, most recent first; or empty when the unit is out of the
   *         caller's reach, as opposed to a present but empty list for a unit never serviced
   */
  Optional<List<ServiceRequest>> handle(ListEquipmentInterventionsQuery query);
}
