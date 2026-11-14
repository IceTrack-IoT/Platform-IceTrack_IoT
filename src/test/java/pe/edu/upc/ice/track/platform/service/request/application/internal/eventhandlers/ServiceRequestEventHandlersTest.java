package pe.edu.upc.ice.track.platform.service.request.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.service.request.application.internal.eventhandlers.EdgeGrantEventHandler;
import pe.edu.upc.ice.track.platform.service.request.application.internal.eventhandlers.ServiceRequestOutboxEventHandler;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.*;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.*;
import pe.edu.upc.ice.track.platform.shared.application.outbox.IntegrationEventOutbox;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestEventHandlersTest {

  private static final Instant AT = Instant.parse("2026-11-12T10:15:30Z");

  @Nested
  class EdgeGrants {

    @Mock private ApplicationEventPublisher publisher;

    @Test
    @DisplayName("on(EdgeGrantIssuedEvent) -> republishes an EdgeGrantIssuedIntegrationEvent with the same data (AAA)")
    void issued() {
      // Arrange
      var handler = new EdgeGrantEventHandler(publisher);

      // Act
      handler.on(new EdgeGrantIssuedEvent(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID));

      // Assert
      verify(publisher, times(1)).publishEvent(
          (Object) new EdgeGrantIssuedIntegrationEvent(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID));
      verifyNoMoreInteractions(publisher);
    }

    @Test
    @DisplayName("on(EdgeGrantRevokedEvent) -> republishes an EdgeGrantRevokedIntegrationEvent carrying the reason (AAA)")
    void revoked() {
      // Arrange
      var handler = new EdgeGrantEventHandler(publisher);

      // Act
      handler.on(new EdgeGrantRevokedEvent(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, "REASSIGNED"));

      // Assert
      verify(publisher, times(1)).publishEvent(
          (Object) new EdgeGrantRevokedIntegrationEvent(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, "REASSIGNED"));
      verifyNoMoreInteractions(publisher);
    }
  }

  @Nested
  class Outbox {

    @Mock private IntegrationEventOutbox outbox;

    @Test
    @DisplayName("on(TechnicianAssignedEvent) -> appends to ServiceRequest aggregate with type and priority as names (AAA)")
    void assigned() {
      // Arrange
      var handler = new ServiceRequestOutboxEventHandler(outbox);

      // Act
      handler.on(new TechnicianAssignedEvent(REQUEST_ID, OWNER_ID, TECHNICIAN_ID, OTHER_TECHNICIAN_ID, EQUIPMENT_ID, SITE_ID,
          ServiceType.REPAIR, ServicePriority.HIGH, AT));

      // Assert
      verify(outbox, times(1)).append(eq("ServiceRequest"), eq(REQUEST_ID), eq(new TechnicianAssignedIntegrationEvent(
          REQUEST_ID, OWNER_ID, TECHNICIAN_ID, OTHER_TECHNICIAN_ID, EQUIPMENT_ID, SITE_ID, "REPAIR", "HIGH", AT)));
      verifyNoMoreInteractions(outbox);
    }

    @Test
    @DisplayName("on(ServiceRequestRejectedEvent) -> appends the rejection integration event (AAA)")
    void rejected() {
      // Arrange
      var handler = new ServiceRequestOutboxEventHandler(outbox);

      // Act
      handler.on(new ServiceRequestRejectedEvent(REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, "busy", AT));

      // Assert
      verify(outbox, times(1)).append(eq("ServiceRequest"), eq(REQUEST_ID),
          eq(new ServiceRequestRejectedIntegrationEvent(REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, "busy", AT)));
      verifyNoMoreInteractions(outbox);
    }

    @Test
    @DisplayName("on(ServiceRequestCanceledEvent) -> appends the cancellation, keeping a null technician (AAA)")
    void canceled() {
      // Arrange
      var handler = new ServiceRequestOutboxEventHandler(outbox);

      // Act
      handler.on(new ServiceRequestCanceledEvent(REQUEST_ID, OWNER_ID, null, EQUIPMENT_ID, "no longer needed", AT));

      // Assert
      verify(outbox, times(1)).append(eq("ServiceRequest"), eq(REQUEST_ID),
          eq(new ServiceRequestCanceledIntegrationEvent(REQUEST_ID, OWNER_ID, null, EQUIPMENT_ID, "no longer needed", AT)));
      verifyNoMoreInteractions(outbox);
    }

    @Test
    @DisplayName("on(ServiceCompletedEvent) -> appends a completion integration event without the client uuid (AAA)")
    void completed() {
      // Arrange
      var handler = new ServiceRequestOutboxEventHandler(outbox);

      // Act
      handler.on(new ServiceCompletedEvent(REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, CLIENT_ID, AT));

      // Assert
      verify(outbox, times(1)).append(eq("ServiceRequest"), eq(REQUEST_ID),
          eq(new ServiceRequestCompletedIntegrationEvent(REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, AT)));
      verifyNoMoreInteractions(outbox);
    }

    @Test
    @DisplayName("on(ReviewSubmittedEvent) -> appends under the Review aggregate keyed by review id (AAA)")
    void reviewSubmitted() {
      // Arrange
      var handler = new ServiceRequestOutboxEventHandler(outbox);

      // Act
      handler.on(new ReviewSubmittedEvent(REVIEW_ID, REQUEST_ID, OWNER_ID, TECHNICIAN_ID, 4.5, AT));

      // Assert
      verify(outbox, times(1)).append(eq("Review"), eq(REVIEW_ID),
          eq(new ReviewSubmittedIntegrationEvent(REVIEW_ID, REQUEST_ID, TECHNICIAN_ID, 4.5, AT)));
      verifyNoMoreInteractions(outbox);
    }

    @Test
    @DisplayName("on(ReviewUpdatedEvent) -> appends under the Review aggregate keyed by review id (AAA)")
    void reviewUpdated() {
      // Arrange
      var handler = new ServiceRequestOutboxEventHandler(outbox);
      var captor = ArgumentCaptor.forClass(Object.class);

      // Act
      handler.on(new ReviewUpdatedEvent(REVIEW_ID, REQUEST_ID, TECHNICIAN_ID, 2.0, AT));

      // Assert
      verify(outbox, times(1)).append(eq("Review"), eq(REVIEW_ID), captor.capture());
      assertThat(captor.getValue()).isEqualTo(new ReviewUpdatedIntegrationEvent(REVIEW_ID, REQUEST_ID, TECHNICIAN_ID, 2.0, AT));
      verifyNoMoreInteractions(outbox);
    }
  }
}
