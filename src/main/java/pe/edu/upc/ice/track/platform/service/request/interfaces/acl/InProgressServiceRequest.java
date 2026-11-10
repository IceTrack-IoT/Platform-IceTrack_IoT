package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

/**
 * Published view of the request that was in progress on a unit at a given instant, returned by
 * {@link ServiceRequestContextFacade#findInProgressServiceRequestAt}.
 *
 * <p>Part of the published language of the {@code service.request} context: plain values only.</p>
 *
 * @param serviceRequestId    the request
 * @param technicianProfileId the technician assigned to it
 */
public record InProgressServiceRequest(Long serviceRequestId, Long technicianProfileId) {
}
