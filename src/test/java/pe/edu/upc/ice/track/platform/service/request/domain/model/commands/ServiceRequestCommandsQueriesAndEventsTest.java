package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.*;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestDisplayDetailsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListServiceRequestsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestFilter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

class ServiceRequestCommandsQueriesAndEventsTest {

  private static final Instant AT = Instant.parse("2026-03-01T10:00:00Z");

  @Nested
  class Commands {

    @Test
    @DisplayName("RecordMaintenanceWindowLinkCommand(...) -> requires every field except the technician (AAA)")
    void record() {
      // Act
      var command = new RecordMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.BUTTON, null, AT, AT);

      // Assert
      assertThat(command.technicianProfileId()).isNull();
      assertThat(command.origin()).isEqualTo(MaintenanceOrigin.BUTTON);
      assertThatThrownBy(() -> new RecordMaintenanceWindowLinkCommand(null, WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.APP, null, AT, AT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new RecordMaintenanceWindowLinkCommand(REQUEST_ID, null, EQUIPMENT_ID, MaintenanceOrigin.APP, null, AT, AT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new RecordMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, null, MaintenanceOrigin.APP, null, AT, AT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new RecordMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, EQUIPMENT_ID, null, null, AT, AT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new RecordMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.APP, null, null, AT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new RecordMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.APP, null, AT, null))
          .hasMessage("serviceRequestId, windowId, equipmentId, origin, windowStartedAt and attributedAt are required");
    }

    @Test
    @DisplayName("CompleteMaintenanceWindowLinkCommand(...) -> requires ids, end and a non-blank reason (AAA)")
    void complete() {
      // Act & Assert
      assertThat(new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, AT, "AUTO").closureReason()).isEqualTo("AUTO");
      assertThatThrownBy(() -> new CompleteMaintenanceWindowLinkCommand(null, WINDOW_ID, AT, "AUTO"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("serviceRequestId, windowId, endedAt and closureReason are required");
      assertThatThrownBy(() -> new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, null, AT, "AUTO")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, null, "AUTO")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, AT, null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, AT, "  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("data-carrier commands -> expose their components (AAA)")
    void carriers() {
      // Act & Assert
      assertThat(new CreateServiceRequestCommand(USERNAME, EQUIPMENT_ID, ServiceType.REPAIR, ServicePriority.LOW, "d", ALERT_ID).sourceAlertId()).isEqualTo(ALERT_ID);
      assertThat(new AssignTechnicianCommand(REQUEST_ID, TECHNICIAN_ID, USERNAME).technicianProfileId()).isEqualTo(TECHNICIAN_ID);
      assertThat(new CancelServiceRequestCommand(REQUEST_ID, USERNAME, "r").reason()).isEqualTo("r");
      assertThat(new RejectServiceRequestCommand(REQUEST_ID, USERNAME, "r").username()).isEqualTo(USERNAME);
      assertThat(new StartServiceRequestCommand(REQUEST_ID, USERNAME, AT).startedAt()).isEqualTo(AT);
      assertThat(new SubmitReviewCommand(REQUEST_ID, USERNAME, 5, 4, 3, "c").efficacy()).isEqualTo(4);
      assertThat(new UpdateReviewCommand(REVIEW_ID, USERNAME, 5, 4, 3, "c").reviewId()).isEqualTo(REVIEW_ID);
      assertThat(new RegisterInterventionCommand(REQUEST_ID, USERNAME, CLIENT_ID, "d", "a", List.of("p"), List.of(), AT).clientGeneratedId()).isEqualTo(CLIENT_ID);
      assertThat(new AcceptServiceRequestCommand(REQUEST_ID, USERNAME).serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(new RegenerateEdgeGrantCommand(REQUEST_ID, USERNAME).username()).isEqualTo(USERNAME);
    }
  }

  @Nested
  class Queries {

    @Test
    @DisplayName("new ListServiceRequestsQuery(nulls) -> defaults OWNER role, NONE filter, page 0, size 20 (AAA)")
    void defaults() {
      // Act
      var query = new ListServiceRequestsQuery(USERNAME, null, null, false, null, null);

      // Assert
      assertThat(query.callerRole()).isEqualTo(CallerRole.OWNER);
      assertThat(query.filter()).isSameAs(ServiceRequestFilter.NONE);
      assertThat(query.page()).isZero();
      assertThat(query.size()).isEqualTo(ListServiceRequestsQuery.DEFAULT_PAGE_SIZE).isEqualTo(20);
    }

    @Test
    @DisplayName("new ListServiceRequestsQuery(size 1 / 100) -> accepted; 0 / 101 / negative page -> rejected (AAA)")
    void bounds() {
      // Act & Assert
      assertThat(new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, null, false, 2, 1).size()).isEqualTo(1);
      assertThat(new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, null, false, 0, 100).size()).isEqualTo(100);
      assertThatThrownBy(() -> new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, null, false, 0, 0))
          .hasMessage("Page size must be between 1 and 100");
      assertThatThrownBy(() -> new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, null, false, 0, 101))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, null, false, -1, 10))
          .hasMessage("Page index must not be negative");
    }

    @Test
    @DisplayName("new ListServiceRequestsQuery(assignedToMe) -> only technicians may ask for it (AAA)")
    void assignedToMe() {
      // Act & Assert
      assertThat(new ListServiceRequestsQuery(USERNAME, CallerRole.TECHNICIAN, null, true, 0, 10).assignedToMe()).isTrue();
      assertThatThrownBy(() -> new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, null, true, 0, 10))
          .hasMessage("assigned_to=me is only available to technicians");
      assertThatThrownBy(() -> new ListServiceRequestsQuery(USERNAME, null, null, true, 0, 10))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new GetServiceRequestDisplayDetailsQuery(null) -> empty immutable list (AAA)")
    void displayQuery() {
      // Act
      var empty = new GetServiceRequestDisplayDetailsQuery(null, true);
      var filled = new GetServiceRequestDisplayDetailsQuery(new java.util.ArrayList<>(List.of(pending())), false);

      // Assert
      assertThat(empty.serviceRequests()).isEmpty();
      assertThat(empty.includeReviews()).isTrue();
      assertThatThrownBy(() -> filled.serviceRequests().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  class Events {

    @Test
    @DisplayName("ServiceRequestRejectedEvent.from(...) -> uses the rejecting technician, not the current one (AAA)")
    void rejected() {
      // Arrange
      var request = assigned();

      // Act
      var event = ServiceRequestRejectedEvent.from(request, technician(), "no time", AT);

      // Assert
      assertThat(event).isEqualTo(new ServiceRequestRejectedEvent(REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, "no time", AT));
    }

    @Test
    @DisplayName("ServiceCompletedEvent / ServiceStartedEvent / AcceptedEvent / CanceledEvent .from(request) -> map request fields (AAA)")
    void fromRequest() {
      // Arrange
      var done = completed();

      // Act
      var completedEvent = ServiceCompletedEvent.from(done);
      var startedEvent = ServiceStartedEvent.from(done);
      var acceptedEvent = ServiceRequestAcceptedEvent.from(done, AT);
      var canceledEvent = ServiceRequestCanceledEvent.from(canceled());

      // Assert
      assertThat(completedEvent.clientGeneratedId()).isEqualTo(CLIENT_ID);
      assertThat(completedEvent.completedAt()).isEqualTo(done.getCompletedAt());
      assertThat(startedEvent.startedAt()).isEqualTo(done.getStartedAt());
      assertThat(acceptedEvent.acceptedAt()).isEqualTo(AT);
      assertThat(canceledEvent.reason()).isEqualTo("No longer needed");
      assertThat(canceledEvent.technicianId()).isEqualTo(TECHNICIAN_ID);
    }

    @Test
    @DisplayName("TechnicianAssignedEvent.from(request, null) -> previous technician null (AAA)")
    void assignedWithoutPrevious() {
      // Act
      var event = TechnicianAssignedEvent.from(assigned(), null);

      // Assert
      assertThat(event.previousTechnicianId()).isNull();
      assertThat(event.ownerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("ReviewSubmittedEvent / ReviewUpdatedEvent .from(review) -> map review fields and the average (AAA)")
    void reviewEvents() {
      // Arrange
      var review = review();

      // Act
      var submitted = ReviewSubmittedEvent.from(review);
      var updated = ReviewUpdatedEvent.from(review, AT);

      // Assert
      assertThat(submitted.averageRating()).isEqualTo(4.0);
      assertThat(submitted.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(updated.updatedAt()).isEqualTo(AT);
      assertThat(updated.reviewId()).isEqualTo(REVIEW_ID);
    }

    @Test
    @DisplayName("ServiceRequestCreatedEvent.from(request) -> maps ids, type, priority and creation time (AAA)")
    void createdEvent() {
      // Act
      var event = ServiceRequestCreatedEvent.from(pending());

      // Assert
      assertThat(event.siteId()).isEqualTo(SITE_ID);
      assertThat(event.type()).isEqualTo(ServiceType.REPAIR);
      assertThat(event.priority()).isEqualTo(ServicePriority.HIGH);
    }
  }
}
