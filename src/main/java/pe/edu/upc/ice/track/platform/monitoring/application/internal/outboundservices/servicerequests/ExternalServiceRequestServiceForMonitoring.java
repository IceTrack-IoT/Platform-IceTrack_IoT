package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.servicerequests;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

/**
 * Outbound service (Anti-Corruption Layer) through which monitoring asks Service Request
 * Management whether a technician is currently working on a unit.
 *
 * <p>A technician reads telemetry, alerts and the alert policy of a unit only while it holds an
 * active assignment on it. This is the only class in {@code monitoring} that knows a
 * {@code service.request} context exists, and it talks to it exclusively through
 * {@link ServiceRequestContextFacade}.</p>
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
}
