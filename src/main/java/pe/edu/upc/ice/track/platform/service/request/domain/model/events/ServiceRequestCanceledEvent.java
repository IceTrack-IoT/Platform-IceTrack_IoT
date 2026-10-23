package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.time.Instant;

/**
 * Domain event published when an owner cancels a service request.
 *
 * @param serviceRequestId the request
 * @param ownerId          the owner who canceled
 * @param technicianId     the technician assigned at the time, {@code null} when there was none
 * @param reason           why the owner canceled
 * @param canceledAt       when the request was canceled
 */
public record ServiceRequestCanceledEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    String reason,
    Instant canceledAt) {

  /**
   * Extracts the event fields from a request that has just been canceled.
   *
   * @param request the canceled request
   * @return the populated event
   */
  public static ServiceRequestCanceledEvent from(ServiceRequest request) {
    return new ServiceRequestCanceledEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        request.getTechnicianId() == null ? null : request.getTechnicianId().technicianProfileId(),
        request.getCancelReason(),
        request.getCanceledAt());
  }
}
