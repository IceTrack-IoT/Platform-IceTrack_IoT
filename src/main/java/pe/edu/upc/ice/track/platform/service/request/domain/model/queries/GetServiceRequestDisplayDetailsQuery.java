package pe.edu.upc.ice.track.platform.service.request.domain.model.queries;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.util.List;

/**
 * Query to resolve what a set of service requests the caller already reached is shown with: the
 * names of their units, sites and technicians and, when asked, which of them were reviewed.
 *
 * <p>It carries requests, not identifiers, on purpose: they were already scoped to the caller by
 * the read that produced them, so this query cannot be used to name anything the caller does not
 * reach.</p>
 *
 * @param serviceRequests the requests to resolve details for; {@code null} means none
 * @param includeReviews  whether to resolve which of them were reviewed
 */
public record GetServiceRequestDisplayDetailsQuery(List<ServiceRequest> serviceRequests, boolean includeReviews) {

  /** Normalizes a missing list to an empty, immutable one. */
  public GetServiceRequestDisplayDetailsQuery {
    serviceRequests = serviceRequests == null ? List.of() : List.copyOf(serviceRequests);
  }
}
