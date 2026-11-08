package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.util.List;

/**
 * Outbound service through which Device Management reads the technician grants an edge gateway
 * must accept.
 *
 * <p>The only class in this context that knows about Service Request Management; it talks to it
 * exclusively through {@link ServiceRequestContextFacade} and translates its published records into
 * this context's own {@link EdgeConfiguration.Grant}.</p>
 */
@Service
public class ExternalServiceRequestServiceForDevice {

  private final ServiceRequestContextFacade serviceRequestContextFacade;

  public ExternalServiceRequestServiceForDevice(ServiceRequestContextFacade serviceRequestContextFacade) {
    this.serviceRequestContextFacade = serviceRequestContextFacade;
  }

  /**
   * @param equipmentId identifier of the equipment
   * @return the grants that currently open the unit's gateway, oldest first
   */
  public List<EdgeConfiguration.Grant> fetchActiveEdgeGrants(Long equipmentId) {
    return serviceRequestContextFacade.fetchActiveEdgeGrants(equipmentId).stream()
        .map(grant -> new EdgeConfiguration.Grant(
            grant.tokenSha256(), grant.technicianProfileId(), grant.serviceRequestId(), grant.expiresAt()))
        .toList();
  }
}
