package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.servicerequests;

/**
 * The order, and the technician working on it, a maintenance window can be attributed to.
 *
 * @param serviceRequestId    the order
 * @param technicianProfileId the technician assigned to it
 */
public record ServiceRequestAttribution(Long serviceRequestId, Long technicianProfileId) {
}
