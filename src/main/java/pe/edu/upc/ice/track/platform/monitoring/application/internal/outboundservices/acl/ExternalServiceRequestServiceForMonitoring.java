package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.time.Instant;
import java.util.Optional;

/**
 * Outbound service (Anti-Corruption Layer) through which monitoring asks Service Request
 * Management whether a technician is currently working on a unit.
 *
 * <p>A technician reads telemetry, alerts and the alert policy of a unit only while it holds an
 * active assignment on it, and a maintenance window started with the hardware button is attributed
 * to the order that was in progress on the unit when it started. This is the only class in
 * {@code monitoring} that knows a {@code service.request} context exists, and it talks to it
 * exclusively through {@link ServiceRequestContextFacade}.</p>
 */
@Service
public class ExternalServiceRequestServiceForMonitoring {

  private final ServiceRequestContextFacade serviceRequestContextFacade;

  public ExternalServiceRequestServiceForMonitoring(ServiceRequestContextFacade serviceRequestContextFacade) {
    this.serviceRequestContextFacade = serviceRequestContextFacade;
  }

  /**
   * @param technicianProfileId identifier of the technician profile
   * @param equipmentId         identifier of the unit
   * @return {@code true} when the technician holds an active assignment on the unit
   */
  public boolean hasActiveAssignmentOnEquipment(Long technicianProfileId, Long equipmentId) {
    return serviceRequestContextFacade.hasActiveAssignmentOnEquipment(technicianProfileId, equipmentId);
  }

  /**
   * Finds the order that was in progress on a unit at an instant.
   *
   * @param equipmentId identifier of the unit
   * @param instant     the instant, typically when a maintenance window started
   * @return the order and its technician, or empty when no order was in progress then
   */
  public Optional<ServiceRequestAttribution> findInProgressServiceRequestAt(Long equipmentId, Instant instant) {
    return serviceRequestContextFacade.findInProgressServiceRequestAt(equipmentId, instant)
        .map(order -> new ServiceRequestAttribution(order.serviceRequestId(), order.technicianProfileId()));
  }
}
