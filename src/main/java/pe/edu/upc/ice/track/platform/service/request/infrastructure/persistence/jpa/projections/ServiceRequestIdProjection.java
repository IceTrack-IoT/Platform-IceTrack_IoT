package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections;

/**
 * Closed projection of a {@code ReviewPersistenceEntity} onto the request it reviews.
 *
 * <p>Being closed, it lets Spring Data select the {@code service_request_id} column alone instead of
 * the whole review.</p>
 */
public interface ServiceRequestIdProjection {

  /** @return the identifier of the reviewed service request */
  Long getServiceRequestId();
}
