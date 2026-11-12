package pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.*;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

class ServiceRequestTest {

  private static ServiceRequest created(String description, Long alertId) {
    return ServiceRequest.create(owner(), new EquipmentId(EQUIPMENT_ID), new SiteId(SITE_ID), ServiceType.PREVENTIVE_MAINTENANCE,
        ServicePriority.LOW, description, alertId);
  }

  @Nested
  class Creation {

    @Test
    @DisplayName("create(...) -> PENDING request without id, trimmed description and a creation transition (AAA)")
    void create() {
      // Arrange
      var before = now();

      // Act
      var request = created("  " + DESCRIPTION + "  ", ALERT_ID);

      // Assert
      assertThat(request.getId()).isNull();
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(request.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(request.getSourceAlertId()).isEqualTo(ALERT_ID);
      assertThat(request.getTechnicianId()).isNull();
      assertThat(request.getCreatedAt()).isBetween(before, now());
      assertThat(request.getHistory()).hasSize(1);
      var creation = request.getHistory().get(0);
      assertThat(creation.isCreation()).isTrue();
      assertThat(creation.getToStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(creation.getActorId()).isEqualTo(OWNER_ID);
      assertThat(request.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("onCreated() on a persisted request -> registers ServiceRequestCreatedEvent (AAA)")
    void onCreated() {
      // Arrange
      var request = pending();

      // Act
      request.onCreated();

      // Assert
      assertThat(request.domainEvents()).containsExactly(new ServiceRequestCreatedEvent(
          REQUEST_ID, OWNER_ID, EQUIPMENT_ID, SITE_ID, ServiceType.REPAIR, ServicePriority.HIGH, request.getCreatedAt()));
    }

    @Test
    @DisplayName("onCreated() before persistence -> IllegalStateException, no event (AAA)")
    void onCreatedWithoutId() {
      // Arrange
      var request = created(DESCRIPTION, null);

      // Act & Assert
      assertThatThrownBy(request::onCreated).isInstanceOf(IllegalStateException.class)
          .hasMessage("A service request must be persisted before its creation is announced");
      assertThat(request.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("new ServiceRequest(blank / oversized description / bad alert / null parts) -> IllegalArgumentException (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> created("   ", null)).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Service request description must not be null or blank");
      assertThatThrownBy(() -> created(null, null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> created("x".repeat(1001), null)).hasMessage("Service request description must not exceed 1000 characters");
      assertThat(created("x".repeat(1000), null).getDescription()).hasSize(1000);
      assertThatThrownBy(() -> created(DESCRIPTION, 0L)).hasMessage("Service request sourceAlertId must be positive");
      assertThatThrownBy(() -> ServiceRequest.create(null, new EquipmentId(1L), new SiteId(1L), ServiceType.REPAIR,
          ServicePriority.LOW, DESCRIPTION, null)).hasMessage("Service request ownerId must not be null");
      assertThatThrownBy(() -> ServiceRequest.create(owner(), null, new SiteId(1L), ServiceType.REPAIR,
          ServicePriority.LOW, DESCRIPTION, null)).hasMessage("Service request equipmentId must not be null");
      assertThatThrownBy(() -> ServiceRequest.create(owner(), new EquipmentId(1L), null, ServiceType.REPAIR,
          ServicePriority.LOW, DESCRIPTION, null)).hasMessage("Service request siteId must not be null");
      assertThatThrownBy(() -> ServiceRequest.create(owner(), new EquipmentId(1L), new SiteId(1L), null,
          ServicePriority.LOW, DESCRIPTION, null)).hasMessage("Service request type must not be null");
      assertThatThrownBy(() -> ServiceRequest.create(owner(), new EquipmentId(1L), new SiteId(1L), ServiceType.REPAIR,
          null, DESCRIPTION, null)).hasMessage("Service request priority must not be null");
    }

    @Test
    @DisplayName("getHistory() -> read-only view of the transitions (AAA)")
    void historyReadOnly() {
      // Arrange
      var history = pending().getHistory();

      // Act & Assert
      assertThatThrownBy(() -> history.add(StatusTransition.of(null, ServiceStatus.PENDING, 1L, now(), null, null)))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  class Assignment {

    @Test
    @DisplayName("assignTechnician(tech, owner) on PENDING -> stays PENDING with technician, assignedAt, transition and event (AAA)")
    void assignFromPending() {
      // Arrange
      var request = pending();

      // Act
      request.assignTechnician(technician(), owner());

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(request.getTechnicianId()).isEqualTo(technician());
      assertThat(request.getAssignedAt()).isNotNull();
      var last = request.getHistory().get(1);
      assertThat(last.getFromStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(last.getToStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(last.getActorId()).isEqualTo(OWNER_ID);
      assertThat(last.getTechnicianId()).isEqualTo(technician());
      assertThat(request.domainEvents()).hasSize(1);
      var event = (TechnicianAssignedEvent) request.domainEvents().iterator().next();
      assertThat(event.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(event.previousTechnicianId()).isNull();
      assertThat(event.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(event.siteId()).isEqualTo(SITE_ID);
      assertThat(event.type()).isEqualTo(ServiceType.REPAIR);
      assertThat(event.priority()).isEqualTo(ServicePriority.HIGH);
    }

    @Test
    @DisplayName("assignTechnician(other tech) on ACCEPTED -> back to PENDING, event carries the previous technician (AAA)")
    void reassignFromAccepted() {
      // Arrange
      var request = accepted();

      // Act
      request.assignTechnician(new TechnicianProfileId(OTHER_TECHNICIAN_ID), owner());

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(request.getTechnicianId().technicianProfileId()).isEqualTo(OTHER_TECHNICIAN_ID);
      var event = (TechnicianAssignedEvent) request.domainEvents().iterator().next();
      assertThat(event.previousTechnicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(event.technicianId()).isEqualTo(OTHER_TECHNICIAN_ID);
    }

    @Test
    @DisplayName("assignTechnician(...) by a different owner -> IllegalArgumentException, state untouched (AAA)")
    void wrongOwner() {
      // Arrange
      var request = pending();

      // Act & Assert
      assertThatThrownBy(() -> request.assignTechnician(technician(), new OwnerId(OTHER_OWNER_ID)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Only the owner who created the service request may perform this operation");
      assertThat(request.getTechnicianId()).isNull();
      assertThat(request.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("assignTechnician(null) -> IllegalArgumentException (AAA)")
    void nullTechnician() {
      // Act & Assert
      assertThatThrownBy(() -> pending().assignTechnician(null, owner()))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Service request technicianId must not be null");
    }

    @Test
    @DisplayName("assignTechnician(...) on IN_PROGRESS / COMPLETED / CANCELED -> IllegalStateException naming the allowed states (AAA)")
    void wrongStatus() {
      // Act & Assert
      assertThatThrownBy(() -> inProgress().assignTechnician(technician(), owner()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot assign a technician to a service request in status IN_PROGRESS; it must be one of [PENDING, ACCEPTED]");
      assertThatThrownBy(() -> completed().assignTechnician(technician(), owner())).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> canceled().assignTechnician(technician(), owner())).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  class Acceptance {

    @Test
    @DisplayName("accept(tech) on an assigned PENDING request -> ACCEPTED with transition and event (AAA)")
    void accept() {
      // Arrange
      var request = assigned();

      // Act
      request.accept(technician());

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.ACCEPTED);
      var last = request.getHistory().get(request.getHistory().size() - 1);
      assertThat(last.getToStatus()).isEqualTo(ServiceStatus.ACCEPTED);
      assertThat(last.getActorId()).isEqualTo(TECHNICIAN_ID);
      var event = (ServiceRequestAcceptedEvent) request.domainEvents().iterator().next();
      assertThat(event.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(event.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(event.acceptedAt()).isEqualTo(last.getOccurredAt());
    }

    @Test
    @DisplayName("accept(tech) by someone not assigned / on a non-PENDING request -> exception (AAA)")
    void guards() {
      // Act & Assert
      assertThatThrownBy(() -> assigned().accept(new TechnicianProfileId(OTHER_TECHNICIAN_ID)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Only the technician assigned to the service request may perform this operation");
      assertThatThrownBy(() -> pending().accept(technician())).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> accepted().accept(technician())).isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot accept a service request in status ACCEPTED; it must be one of [PENDING]");
    }

    @Test
    @DisplayName("reject(tech, reason) -> PENDING again, technician cleared, reason trimmed, event names the rejecting technician (AAA)")
    void reject() {
      // Arrange
      var request = assigned();

      // Act
      request.reject(technician(), "  Too far away  ");

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(request.getTechnicianId()).isNull();
      assertThat(request.getAssignedAt()).isNull();
      var last = request.getHistory().get(request.getHistory().size() - 1);
      assertThat(last.getReason()).isEqualTo("Too far away");
      assertThat(last.getTechnicianId()).isEqualTo(technician());
      var event = (ServiceRequestRejectedEvent) request.domainEvents().iterator().next();
      assertThat(event.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(event.reason()).isEqualTo("Too far away");
    }

    @Test
    @DisplayName("reject(tech, blank / oversized reason) and on a non-PENDING request -> exception (AAA)")
    void rejectGuards() {
      // Act & Assert
      assertThatThrownBy(() -> assigned().reject(technician(), " ")).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("A rejection reason is required");
      assertThatThrownBy(() -> assigned().reject(technician(), null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> assigned().reject(technician(), "x".repeat(501)))
          .hasMessage("The rejection reason must not exceed 500 characters");
      assertThatThrownBy(() -> accepted().reject(technician(), "no")).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> assigned().reject(new TechnicianProfileId(OTHER_TECHNICIAN_ID), "no"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Starting {

    @Test
    @DisplayName("start(tech, null) on ACCEPTED -> IN_PROGRESS starting now, with event (AAA)")
    void startNow() {
      // Arrange
      var request = accepted();
      var before = now();

      // Act
      request.start(technician(), null);

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.IN_PROGRESS);
      assertThat(request.getStartedAt()).isBetween(before, now());
      var event = (ServiceStartedEvent) request.domainEvents().iterator().next();
      assertThat(event.startedAt()).isEqualTo(request.getStartedAt());
      assertThat(event.equipmentId()).isEqualTo(EQUIPMENT_ID);
    }

    @Test
    @DisplayName("start(tech, deviceTime) -> device time (truncated to micros) becomes the start and the transition instant (AAA)")
    void startWithDeviceTime() {
      // Arrange
      var request = accepted();
      var device = Instant.now().minusSeconds(600).plusNanos(123);

      // Act
      request.start(technician(), device);

      // Assert
      assertThat(request.getStartedAt()).isEqualTo(device.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
      var last = request.getHistory().get(request.getHistory().size() - 1);
      assertThat(last.getOccurredAt()).isEqualTo(request.getStartedAt());
    }

    @Test
    @DisplayName("start(tech, device time within clock skew ahead) -> accepted (AAA)")
    void startWithinSkew() {
      // Arrange
      var request = accepted();

      // Act
      request.start(technician(), Instant.now().plus(Duration.ofMinutes(4)));

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("start(tech, device time beyond clock skew ahead) -> IllegalArgumentException, status untouched (AAA)")
    void startInFuture() {
      // Arrange
      var request = accepted();

      // Act & Assert
      assertThatThrownBy(() -> request.start(technician(), Instant.now().plus(Duration.ofMinutes(6))))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Service request startedAt must not lie in the future");
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.ACCEPTED);
      assertThat(request.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("start(tech, device time just past the 5 min skew) -> IllegalArgumentException (AAA)")
    void startJustBeyondSkew() {
      // Arrange
      var request = accepted();

      // Act & Assert
      assertThatThrownBy(() -> request.start(technician(), Instant.now().plus(ServiceRequest.MAX_DEVICE_CLOCK_SKEW).plusSeconds(1)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.ACCEPTED);
    }

    @Test
    @DisplayName("start(tech, same device time) on an IN_PROGRESS request -> idempotent replay, no new transition or event (AAA)")
    void startIsIdempotent() {
      // Arrange
      var request = accepted();
      var device = Instant.now().minusSeconds(120);
      request.start(technician(), device);
      request.clearDomainEvents();
      var transitions = request.getHistory().size();

      // Act
      request.start(technician(), device);

      // Assert
      assertThat(request.getHistory()).hasSize(transitions);
      assertThat(request.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("start(tech, other time) on IN_PROGRESS or start by other tech or from PENDING -> exception (AAA)")
    void startGuards() {
      // Act & Assert
      assertThatThrownBy(() -> inProgress().start(technician(), Instant.now().minusSeconds(5)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot start a service request in status IN_PROGRESS; it must be one of [ACCEPTED]");
      assertThatThrownBy(() -> assigned().start(technician(), null)).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> accepted().start(new TechnicianProfileId(OTHER_TECHNICIAN_ID), null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Intervention {

    @Test
    @DisplayName("registerIntervention(...) on IN_PROGRESS -> COMPLETED with intervention, completion time and event (AAA)")
    void register() {
      // Arrange
      var request = inProgress();
      var completedAt = now().minusSeconds(30);

      // Act
      var intervention = request.registerIntervention(technician(), CLIENT_ID, " Worn bearing ", "Replaced",
          List.of(" bearing "), List.of("p.jpg"), completedAt);

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.COMPLETED);
      assertThat(request.getIntervention()).isSameAs(intervention);
      assertThat(intervention.getDiagnosis()).isEqualTo("Worn bearing");
      assertThat(intervention.getSpareParts()).containsExactly("bearing");
      assertThat(intervention.getStartedAt()).isEqualTo(request.getStartedAt());
      assertThat(request.getCompletedAt()).isEqualTo(completedAt);
      var event = (ServiceCompletedEvent) request.domainEvents().iterator().next();
      assertThat(event.clientGeneratedId()).isEqualTo(CLIENT_ID);
      assertThat(event.completedAt()).isEqualTo(completedAt);
      assertThat(event.technicianId()).isEqualTo(TECHNICIAN_ID);
    }

    @Test
    @DisplayName("registerIntervention(..., null completedAt) -> completion defaults to now, never before the start (AAA)")
    void defaultsCompletion() {
      // Arrange
      var request = inProgress();
      var before = now();

      // Act
      request.registerIntervention(technician(), CLIENT_ID, "d", "a", null, null, null);

      // Assert
      assertThat(request.getCompletedAt()).isBetween(before, now());
      assertThat(request.getIntervention().getSpareParts()).isEmpty();
      assertThat(request.getIntervention().getPhotoEvidence()).isEmpty();
    }

    @Test
    @DisplayName("registerIntervention(...) replayed with the same client id -> returns the stored intervention without changes (AAA)")
    void replay() {
      // Arrange
      var request = completed();
      var stored = request.getIntervention();
      var transitions = request.getHistory().size();

      // Act
      var replayed = request.registerIntervention(technician(), CLIENT_ID, "other", "other", null, null, null);

      // Assert
      assertThat(replayed).isSameAs(stored);
      assertThat(request.getHistory()).hasSize(transitions);
      assertThat(request.domainEvents()).isEmpty();
      assertThat(request.hasInterventionIdentifiedBy(CLIENT_ID)).isTrue();
      assertThat(request.hasInterventionIdentifiedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("registerIntervention(...) with another client id on a COMPLETED request -> IllegalStateException (AAA)")
    void secondDifferentIntervention() {
      // Act & Assert
      assertThatThrownBy(() -> completed().registerIntervention(technician(), UUID.randomUUID(), "d", "a", null, null, null))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot register an intervention for a service request in status COMPLETED; it must be one of [IN_PROGRESS]");
    }

    @Test
    @DisplayName("registerIntervention(...) guards -> null client id, wrong technician, wrong status, future or earlier completion (AAA)")
    void guards() {
      // Arrange
      var running = inProgress();

      // Act & Assert
      assertThatThrownBy(() -> running.registerIntervention(technician(), null, "d", "a", null, null, null))
          .hasMessage("Service request clientGeneratedId must not be null");
      assertThatThrownBy(() -> running.registerIntervention(new TechnicianProfileId(OTHER_TECHNICIAN_ID), CLIENT_ID, "d", "a", null, null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> accepted().registerIntervention(technician(), CLIENT_ID, "d", "a", null, null, null))
          .isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> running.registerIntervention(technician(), CLIENT_ID, "d", "a", null, null, Instant.now().plus(Duration.ofMinutes(10))))
          .hasMessage("Service request completedAt must not lie in the future");
      assertThatThrownBy(() -> running.registerIntervention(technician(), CLIENT_ID, "d", "a", null, null, running.getStartedAt().minusSeconds(60)))
          .hasMessage("An intervention cannot be completed before it started");
      assertThat(running.getStatus()).isEqualTo(ServiceStatus.IN_PROGRESS);
      assertThat(running.getIntervention()).isNull();
      assertThat(running.domainEvents()).isEmpty();
    }
  }

  @Nested
  class Cancellation {

    @Test
    @DisplayName("cancel(owner, reason) on PENDING -> CANCELED with trimmed reason, timestamp and event (AAA)")
    void cancelPending() {
      // Arrange
      var request = pending();

      // Act
      request.cancel(owner(), "  Not needed  ");

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.CANCELED);
      assertThat(request.getCancelReason()).isEqualTo("Not needed");
      assertThat(request.getCanceledAt()).isNotNull();
      var event = (ServiceRequestCanceledEvent) request.domainEvents().iterator().next();
      assertThat(event.technicianId()).isNull();
      assertThat(event.reason()).isEqualTo("Not needed");
      assertThat(event.canceledAt()).isEqualTo(request.getCanceledAt());
    }

    @Test
    @DisplayName("cancel(owner, reason) on ACCEPTED -> event keeps the assigned technician (AAA)")
    void cancelAccepted() {
      // Arrange
      var request = accepted();

      // Act
      request.cancel(owner(), "Changed plans");

      // Assert
      var event = (ServiceRequestCanceledEvent) request.domainEvents().iterator().next();
      assertThat(event.technicianId()).isEqualTo(TECHNICIAN_ID);
    }

    @Test
    @DisplayName("cancel(...) guards -> wrong owner, IN_PROGRESS / COMPLETED / CANCELED, blank or oversized reason (AAA)")
    void guards() {
      // Act & Assert
      assertThatThrownBy(() -> pending().cancel(new OwnerId(OTHER_OWNER_ID), "x")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> inProgress().cancel(owner(), "x")).isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot cancel a service request in status IN_PROGRESS; it must be one of [PENDING, ACCEPTED]");
      assertThatThrownBy(() -> completed().cancel(owner(), "x")).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> canceled().cancel(owner(), "x")).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> pending().cancel(owner(), "  ")).hasMessage("A cancellation reason is required");
      assertThatThrownBy(() -> pending().cancel(owner(), "x".repeat(501)))
          .hasMessage("The cancellation reason must not exceed 500 characters");
      assertThat(pending().getCanceledAt()).isNull();
    }
  }

  @Nested
  class MaintenanceWindows {

    @Test
    @DisplayName("linkMaintenanceWindow(...) while IN_PROGRESS -> true and the link is stored open (AAA)")
    void link() {
      // Arrange
      var request = inProgress();
      var attributed = request.getStartedAt().plusSeconds(10);

      // Act
      var linked = request.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, attributed, attributed);

      // Assert
      assertThat(linked).isTrue();
      assertThat(request.getMaintenanceWindows()).hasSize(1);
      var link = request.getMaintenanceWindows().get(0);
      assertThat(link.getWindowId()).isEqualTo(WINDOW_ID);
      assertThat(link.getOrigin()).isEqualTo(MaintenanceOrigin.APP);
      assertThat(link.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("linkMaintenanceWindow(...) twice with the same window -> second call false, single link (AAA)")
    void linkTwice() {
      // Arrange
      var request = inProgress();
      var at = request.getStartedAt().plusSeconds(10);
      request.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, at, at);

      // Act
      var again = request.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.BUTTON, null, at, at);

      // Assert
      assertThat(again).isFalse();
      assertThat(request.getMaintenanceWindows()).hasSize(1);
    }

    @Test
    @DisplayName("linkMaintenanceWindow(...) outside the in-progress period -> IllegalStateException; null window -> IllegalArgumentException (AAA)")
    void linkGuards() {
      // Arrange
      var running = inProgress();
      var before = running.getStartedAt().minusSeconds(1);

      // Act & Assert
      assertThatThrownBy(() -> running.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, null, before, before))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageStartingWith("Service request 77 was not IN_PROGRESS at ")
          .hasMessageEndingWith("so maintenance window 300 cannot be linked to it");
      assertThatThrownBy(() -> accepted().linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, null, now(), now()))
          .isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> running.linkMaintenanceWindow(null, MaintenanceOrigin.APP, null, now(), now()))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Service request maintenance window must not be null");
    }

    @Test
    @DisplayName("getMaintenanceWindows() -> ordered by start then window id (AAA)")
    void ordering() {
      // Arrange
      var request = inProgress();
      var base = request.getStartedAt();
      request.linkMaintenanceWindow(3L, MaintenanceOrigin.APP, null, base.plusSeconds(20), base.plusSeconds(20));
      request.linkMaintenanceWindow(2L, MaintenanceOrigin.APP, null, base.plusSeconds(10), base.plusSeconds(10));
      request.linkMaintenanceWindow(1L, MaintenanceOrigin.APP, null, base.plusSeconds(10), base.plusSeconds(10));

      // Act & Assert
      assertThat(request.getMaintenanceWindows()).extracting("windowId").containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("completeMaintenanceWindowLink(...) -> true once, false when already closed or unknown (AAA)")
    void complete() {
      // Arrange
      var request = inProgress();
      var at = request.getStartedAt().plusSeconds(10);
      request.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, null, at, at);

      // Act & Assert
      assertThat(request.completeMaintenanceWindowLink(WINDOW_ID, at.plusSeconds(60), "COMPLETED")).isTrue();
      assertThat(request.completeMaintenanceWindowLink(WINDOW_ID, at.plusSeconds(90), "COMPLETED")).isFalse();
      assertThat(request.completeMaintenanceWindowLink(999L, at.plusSeconds(60), "COMPLETED")).isFalse();
      assertThat(request.getMaintenanceWindows().get(0).getEndedAt()).isEqualTo(at.plusSeconds(60));
    }

    @Test
    @DisplayName("completeMaintenanceWindowLink(..., blank reason) -> IllegalArgumentException (AAA)")
    void completeNeedsReason() {
      // Arrange
      var request = inProgress();
      var at = request.getStartedAt().plusSeconds(10);
      request.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, null, at, at);

      // Act & Assert
      assertThatThrownBy(() -> request.completeMaintenanceWindowLink(WINDOW_ID, at, " "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("A closure reason is required to complete a maintenance window link");
    }

    @Test
    @DisplayName("wasInProgressAt(instant) -> true only between start and completion of a started request (AAA)")
    void wasInProgress() {
      // Arrange
      var running = inProgress();
      var done = completed();

      // Act & Assert
      assertThat(running.wasInProgressAt(running.getStartedAt())).isTrue();
      assertThat(running.wasInProgressAt(running.getStartedAt().minusSeconds(1))).isFalse();
      assertThat(running.wasInProgressAt(now().plusSeconds(600))).isTrue();
      assertThat(done.wasInProgressAt(done.getStartedAt().plusSeconds(1))).isTrue();
      assertThat(done.wasInProgressAt(done.getCompletedAt())).isFalse();
      assertThat(running.wasInProgressAt(null)).isFalse();
      assertThat(accepted().wasInProgressAt(now())).isFalse();
      assertThat(canceled().wasInProgressAt(now())).isFalse();
    }
  }

  @Nested
  class Queries {

    @Test
    @DisplayName("hasActiveTechnician() -> technician present and status PENDING / ACCEPTED / IN_PROGRESS (AAA)")
    void activeTechnician() {
      // Act & Assert
      assertThat(pending().hasActiveTechnician()).isFalse();
      assertThat(assigned().hasActiveTechnician()).isTrue();
      assertThat(inProgress().hasActiveTechnician()).isTrue();
      assertThat(completed().hasActiveTechnician()).isFalse();
      assertThat(canceled().hasActiveTechnician()).isFalse();
    }

    @Test
    @DisplayName("isOwnedBy / isAssignedTo -> match only the exact ids (AAA)")
    void ownership() {
      // Act & Assert
      assertThat(pending().isOwnedBy(owner())).isTrue();
      assertThat(pending().isOwnedBy(new OwnerId(OTHER_OWNER_ID))).isFalse();
      assertThat(pending().isAssignedTo(technician())).isFalse();
      assertThat(assigned().isAssignedTo(technician())).isTrue();
      assertThat(assigned().isAssignedTo(new TechnicianProfileId(OTHER_TECHNICIAN_ID))).isFalse();
      assertThat(assigned().isAssignedTo(null)).isFalse();
      assertThat(pending().getId()).isEqualTo(new ServiceRequestId(REQUEST_ID));
    }
  }
}
