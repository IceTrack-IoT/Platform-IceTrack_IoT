package pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

/**
 * Outbound service through which the {@code profiles} bounded context asks Service Request
 * Management whether an owner and a technician are currently working together.
 *
 * <p>Contact details of a profile (email, phone) are personal data: they are shown to the other
 * party only while a request created by the owner is actively assigned to the technician. This
 * is the only class in {@code profiles} that knows a {@code service.request} context exists, and
 * it talks to it exclusively through {@link ServiceRequestContextFacade}.</p>
 */
@Service
public class ExternalServiceRequestService {

  private final ServiceRequestContextFacade serviceRequestContextFacade;

  public ExternalServiceRequestService(ServiceRequestContextFacade serviceRequestContextFacade) {
    this.serviceRequestContextFacade = serviceRequestContextFacade;
  }

  /**
   * @param ownerProfileId      identifier of the owner profile
   * @param technicianProfileId identifier of the technician profile
   * @return {@code true} while a request of that owner is actively assigned to that technician
   */
  public boolean hasActiveEngagementBetween(Long ownerProfileId, Long technicianProfileId) {
    return serviceRequestContextFacade.hasActiveEngagementBetween(ownerProfileId, technicianProfileId);
  }
}
