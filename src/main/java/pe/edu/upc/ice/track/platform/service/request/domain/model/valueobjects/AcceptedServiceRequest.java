package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.util.Objects;

/**
 * Outcome of accepting a service request: the accepted request and the edge grant issued to its
 * technician in the same transaction.
 *
 * @param serviceRequest the accepted request
 * @param edgeGrant      the grant issued with it, carrying its plaintext token
 */
public record AcceptedServiceRequest(ServiceRequest serviceRequest, IssuedEdgeGrant edgeGrant) {

  public AcceptedServiceRequest {
    Objects.requireNonNull(serviceRequest, "serviceRequest must not be null");
    Objects.requireNonNull(edgeGrant, "edgeGrant must not be null");
  }
}
