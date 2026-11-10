package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

/**
 * Published count of the service requests of an owner that hold one status, returned by
 * {@link ServiceRequestContextFacade#countServiceRequestsByStatus}.
 *
 * <p>Part of the published language of the {@code service.request} context: plain values only. The
 * status travels as the name of its constant, and {@code active} tells whether a request in that
 * status is still open work, so a caller can total the active requests without knowing the
 * lifecycle.</p>
 *
 * @param status the status, by its constant name
 * @param active whether a request in this status is still pending, accepted or in progress
 * @param count  how many requests of the owner hold it
 */
public record ServiceRequestStatusCount(String status, boolean active, long count) {
}
