package pe.edu.upc.ice.track.platform.notifications.infrastructure.servicerequest.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestCanceledCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestRejectedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyTechnicianAssignedCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestCanceledIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestRejectedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.TechnicianAssignedIntegrationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestNotificationEventListenerTest {

  @Mock
  private NotificationCommandService notificationCommandService;

  @InjectMocks
  private ServiceRequestNotificationEventListener listener;

  @Test
  @DisplayName("on(TechnicianAssignedIntegrationEvent) -> dispatches the assignment command with every field (AAA)")
  void assigned() {
    // Arrange
    var event = new TechnicianAssignedIntegrationEvent(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID,
        PREVIOUS_TECHNICIAN_ID, EQUIPMENT_ID, SITE_ID, "REPAIR", "HIGH", T0);

    // Act
    listener.on(event);

    // Assert
    var captor = ArgumentCaptor.forClass(NotifyTechnicianAssignedCommand.class);
    verify(notificationCommandService, times(1)).handle(captor.capture());
    var command = captor.getValue();
    assertThat(command.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(command.technicianProfileId()).isEqualTo(TECHNICIAN_ID);
    assertThat(command.previousTechnicianProfileId()).isEqualTo(PREVIOUS_TECHNICIAN_ID);
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.siteId()).isEqualTo(SITE_ID);
    assertThat(command.serviceType()).isEqualTo("REPAIR");
    assertThat(command.priority()).isEqualTo("HIGH");
    assertThat(command.assignedAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("on(ServiceRequestRejectedIntegrationEvent) -> dispatches the rejection command (AAA)")
  void rejected() {
    // Arrange
    var event = new ServiceRequestRejectedIntegrationEvent(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID,
        EQUIPMENT_ID, "No parts", T0);

    // Act
    listener.on(event);

    // Assert
    var captor = ArgumentCaptor.forClass(NotifyServiceRequestRejectedCommand.class);
    verify(notificationCommandService, times(1)).handle(captor.capture());
    var command = captor.getValue();
    assertThat(command.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(command.ownerProfileId()).isEqualTo(OWNER_ID);
    assertThat(command.technicianProfileId()).isEqualTo(TECHNICIAN_ID);
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.reason()).isEqualTo("No parts");
  }

  @Test
  @DisplayName("on(ServiceRequestCanceledIntegrationEvent) with technician -> dispatches the cancellation command (AAA)")
  void canceledWithTechnician() {
    // Arrange
    var event = new ServiceRequestCanceledIntegrationEvent(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID,
        EQUIPMENT_ID, "Owner canceled", T0);

    // Act
    listener.on(event);

    // Assert
    var captor = ArgumentCaptor.forClass(NotifyServiceRequestCanceledCommand.class);
    verify(notificationCommandService, times(1)).handle(captor.capture());
    var command = captor.getValue();
    assertThat(command.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(command.technicianProfileId()).isEqualTo(TECHNICIAN_ID);
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.reason()).isEqualTo("Owner canceled");
  }

  @Test
  @DisplayName("on(ServiceRequestCanceledIntegrationEvent) without technician -> nobody to notify (AAA)")
  void canceledWithoutTechnician() {
    // Arrange
    var event = new ServiceRequestCanceledIntegrationEvent(SERVICE_REQUEST_ID, OWNER_ID, null,
        EQUIPMENT_ID, "Owner canceled", T0);

    // Act
    listener.on(event);

    // Assert
    verifyNoInteractions(notificationCommandService);
  }
}
