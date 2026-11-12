package pe.edu.upc.ice.track.platform.service.request.test.fixtures;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CreateServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegisterInterventionCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;

import java.time.Instant;
import java.util.List;

import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

public final class ServiceRequestCommandFixtures {

  private ServiceRequestCommandFixtures() {
  }

  public static CreateServiceRequestCommand createCommand(Long sourceAlertId) {
    return new CreateServiceRequestCommand(USERNAME, EQUIPMENT_ID, ServiceType.REPAIR, ServicePriority.HIGH, DESCRIPTION, sourceAlertId);
  }

  public static RegisterInterventionCommand interventionCommand(java.util.UUID clientId) {
    return new RegisterInterventionCommand(REQUEST_ID, USERNAME, clientId, "Worn bearing", "Replaced bearing",
        List.of("bearing"), List.of("photo.jpg"), Instant.now().minusSeconds(30));
  }

  /** Simulates what the repository does on save: the aggregate comes back carrying its identifier. */
  public static ServiceRequest withId(ServiceRequest request) {
    return new ServiceRequest(new ServiceRequestId(REQUEST_ID), request.getOwnerId(), request.getEquipmentId(),
        request.getSiteId(), request.getTechnicianId(), request.getType(), request.getPriority(), request.getDescription(),
        request.getSourceAlertId(), request.getStatus(), request.getCreatedAt(), request.getAssignedAt(),
        request.getStartedAt(), request.getCompletedAt(), request.getCanceledAt(), request.getCancelReason(),
        request.getVersion(), request.getHistory(), request.getIntervention(), request.getMaintenanceWindows());
  }
}
