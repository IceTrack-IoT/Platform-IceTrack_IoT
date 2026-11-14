package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.*;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

class ServiceRequestRestAssemblersTest {

  private static final Instant AT = Instant.parse("2026-11-12T10:15:30Z");

  @Nested
  class EnumResources {

    @Test
    @DisplayName("toDomain() / fromDomain(x) -> every status, type and priority round-trips by name (AAA)")
    void roundTrips() {
      // Act & Assert
      for (var status : ServiceStatus.values()) {
        assertThat(ServiceStatusResource.fromDomain(status).toDomain()).isEqualTo(status);
      }
      for (var type : ServiceType.values()) {
        assertThat(ServiceTypeResource.fromDomain(type).toDomain()).isEqualTo(type);
      }
      for (var priority : ServicePriority.values()) {
        assertThat(ServicePriorityResource.fromDomain(priority).toDomain()).isEqualTo(priority);
      }
      assertThat(ServiceStatusResource.values()).hasSize(ServiceStatus.values().length);
      assertThat(ServiceTypeResource.values()).hasSize(ServiceType.values().length);
      assertThat(ServicePriorityResource.values()).hasSize(ServicePriority.values().length);
    }

    @Test
    @DisplayName("ServiceStatusResource.fromDomain(null) -> null (AAA)")
    void nullStatus() {
      // Act & Assert
      assertThat(ServiceStatusResource.fromDomain(null)).isNull();
    }
  }

  @Nested
  class Commands {

    @Test
    @DisplayName("toCommandFromResource(...) -> each assembler maps path ids, username and body fields in order (AAA)")
    void mapsCommands() {
      // Arrange
      var create = new CreateServiceRequestResource(EQUIPMENT_ID, ServiceTypeResource.REPAIR, ServicePriorityResource.URGENT, DESCRIPTION, ALERT_ID);
      var register = new RegisterInterventionResource(REQUEST_ID, CLIENT_ID, "d", "a", List.of("p"), List.of("u"), AT);

      // Act & Assert
      assertThat(CreateServiceRequestCommandFromResourceAssembler.toCommandFromResource(USERNAME, create))
          .isEqualTo(new CreateServiceRequestCommand(USERNAME, EQUIPMENT_ID, ServiceType.REPAIR, ServicePriority.URGENT, DESCRIPTION, ALERT_ID));
      assertThat(AssignTechnicianCommandFromResourceAssembler.toCommandFromResource(
          REQUEST_ID, USERNAME, new AssignTechnicianResource(TECHNICIAN_ID)))
          .isEqualTo(new AssignTechnicianCommand(REQUEST_ID, TECHNICIAN_ID, USERNAME));
      assertThat(CancelServiceRequestCommandFromResourceAssembler.toCommandFromResource(
          REQUEST_ID, USERNAME, new CancelServiceRequestResource("why")))
          .isEqualTo(new CancelServiceRequestCommand(REQUEST_ID, USERNAME, "why"));
      assertThat(RejectServiceRequestCommandFromResourceAssembler.toCommandFromResource(
          REQUEST_ID, USERNAME, new RejectServiceRequestResource("busy")))
          .isEqualTo(new RejectServiceRequestCommand(REQUEST_ID, USERNAME, "busy"));
      assertThat(RegisterInterventionCommandFromResourceAssembler.toCommandFromResource(USERNAME, register))
          .isEqualTo(new RegisterInterventionCommand(REQUEST_ID, USERNAME, CLIENT_ID, "d", "a", List.of("p"), List.of("u"), AT));
      assertThat(SubmitReviewCommandFromResourceAssembler.toCommandFromResource(
          USERNAME, new SubmitReviewResource(REQUEST_ID, 5, 4, 3, "ok")))
          .isEqualTo(new SubmitReviewCommand(REQUEST_ID, USERNAME, 5, 4, 3, "ok"));
      assertThat(UpdateReviewCommandFromResourceAssembler.toCommandFromResource(
          REVIEW_ID, USERNAME, new UpdateReviewResource(1, 2, 3, "c")))
          .isEqualTo(new UpdateReviewCommand(REVIEW_ID, USERNAME, 1, 2, 3, "c"));
    }

    @Test
    @DisplayName("StartServiceRequestCommandFromResourceAssembler -> startedAt from body; absent body -> null startedAt (AAA)")
    void start() {
      // Act & Assert
      assertThat(StartServiceRequestCommandFromResourceAssembler.toCommandFromResource(
          REQUEST_ID, USERNAME, new StartServiceRequestResource(AT)))
          .isEqualTo(new StartServiceRequestCommand(REQUEST_ID, USERNAME, AT));
      assertThat(StartServiceRequestCommandFromResourceAssembler.toCommandFromResource(REQUEST_ID, USERNAME, null))
          .isEqualTo(new StartServiceRequestCommand(REQUEST_ID, USERNAME, null));
    }
  }

  @Nested
  class Resources {

    @Test
    @DisplayName("ReviewResourceFromEntityAssembler -> ISO instants and average rounded to two decimals (AAA)")
    void reviewResource() {
      // Arrange
      var review = review();

      // Act
      var resource = ReviewResourceFromEntityAssembler.toResourceFromEntity(review);

      // Assert
      assertThat(resource.id()).isEqualTo(REVIEW_ID);
      assertThat(resource.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(resource.ownerId()).isEqualTo(OWNER_ID);
      assertThat(resource.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.communication()).isEqualTo(5);
      assertThat(resource.efficacy()).isEqualTo(4);
      assertThat(resource.performance()).isEqualTo(3);
      assertThat(resource.averageRating()).isEqualTo(4.0);
      assertThat(resource.comment()).isEqualTo("Good job");
      assertThat(resource.createdAt()).isEqualTo(review.getCreatedAt().toString());
      assertThat(resource.editableUntil()).isEqualTo(review.getEditableUntil().toString());
    }

    @Test
    @DisplayName("ReviewResourceFromEntityAssembler -> 4.666... average shown as 4.67 (AAA)")
    void reviewRounding() {
      // Arrange
      var base = review();
      var rounded = new pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review(base.getId(), base.getServiceRequestId(), base.getOwnerId(),
          base.getTechnicianId(), new pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewRating(5, 4, 5), null, base.getCreatedAt(), base.getEditableUntil());

      // Act & Assert
      assertThat(ReviewResourceFromEntityAssembler.toResourceFromEntity(rounded).averageRating()).isEqualTo(4.67);
      assertThat(ReviewResourceFromEntityAssembler.toResourceFromEntity(rounded).comment()).isNull();
    }

    @Test
    @DisplayName("InterventionResourceFromEntityAssembler -> report fields, ISO instants and duration in seconds (AAA)")
    void interventionResource() {
      // Arrange
      var completed = completed();
      var intervention = completed.getIntervention();

      // Act
      var resource = InterventionResourceFromEntityAssembler.toResourceFromEntity(completed);

      // Assert
      assertThat(resource.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(resource.clientGeneratedId()).isEqualTo(intervention.getClientGeneratedId());
      assertThat(resource.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.diagnosis()).isEqualTo(intervention.getDiagnosis());
      assertThat(resource.actionsTaken()).isEqualTo(intervention.getActionsTaken());
      assertThat(resource.spareParts()).isEqualTo(intervention.getSpareParts());
      assertThat(resource.photoEvidence()).isEqualTo(intervention.getPhotoEvidence());
      assertThat(resource.startedAt()).isEqualTo(intervention.getStartedAt().toString());
      assertThat(resource.completedAt()).isEqualTo(intervention.getCompletedAt().toString());
      assertThat(resource.durationSeconds()).isEqualTo(intervention.duration().toSeconds());
    }

    @Test
    @DisplayName("IssuedEdgeGrantResourceFromEntityAssembler -> plaintext token with the grant's ids and ISO instants (AAA)")
    void issuedGrant() {
      // Arrange
      var grant = grant();

      // Act
      var resource = IssuedEdgeGrantResourceFromEntityAssembler.toResourceFromEntity(new IssuedEdgeGrant(grant, "it_grt_secret"));

      // Assert
      assertThat(resource.token()).isEqualTo("it_grt_secret");
      assertThat(resource.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(resource.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.issuedAt()).isEqualTo(grant.getIssuedAt().toString());
      assertThat(resource.expiresAt()).isEqualTo(grant.getExpiresAt().toString());
    }
  }

  @Nested
  class ServiceRequests {

    private final ServiceRequestDisplayDetails details = new ServiceRequestDisplayDetails(
        Map.of(EQUIPMENT_ID, "Freezer 1"), Map.of(SITE_ID, "Lima"), Map.of(TECHNICIAN_ID, "Luis Rojas"), Set.of(REQUEST_ID));

    @Test
    @DisplayName("toResourceFromEntity(pending, NONE) -> unassigned request with null names, one history entry and no edge grant (AAA)")
    void pendingResource() {
      // Arrange
      var request = pending();

      // Act
      var resource = ServiceRequestResourceFromEntityAssembler.toResourceFromEntity(request, null);

      // Assert
      assertThat(resource.id()).isEqualTo(request.getId().serviceRequestId());
      assertThat(resource.ownerId()).isEqualTo(OWNER_ID);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.siteId()).isEqualTo(SITE_ID);
      assertThat(resource.technicianId()).isNull();
      assertThat(resource.equipmentName()).isNull();
      assertThat(resource.siteName()).isNull();
      assertThat(resource.technicianName()).isNull();
      assertThat(resource.type()).isEqualTo(ServiceTypeResource.REPAIR);
      assertThat(resource.priority()).isEqualTo(ServicePriorityResource.HIGH);
      assertThat(resource.status()).isEqualTo(ServiceStatusResource.PENDING);
      assertThat(resource.description()).isEqualTo(DESCRIPTION);
      assertThat(resource.createdAt()).isEqualTo(request.getCreatedAt().toString());
      assertThat(resource.assignedAt()).isNull();
      assertThat(resource.startedAt()).isNull();
      assertThat(resource.completedAt()).isNull();
      assertThat(resource.canceledAt()).isNull();
      assertThat(resource.intervention()).isNull();
      assertThat(resource.edgeGrant()).isNull();
      assertThat(resource.maintenanceWindows()).isEmpty();
      assertThat(resource.history()).hasSize(1);
      assertThat(resource.history().get(0).fromStatus()).isNull();
      assertThat(resource.history().get(0).toStatus()).isEqualTo(ServiceStatusResource.PENDING);
    }

    @Test
    @DisplayName("toResourceFromEntity(completed, details) -> names, intervention and windows included (AAA)")
    void completedResource() {
      // Arrange
      var request = completed();
      request.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, request.getStartedAt(), request.getStartedAt());

      // Act
      var resource = ServiceRequestResourceFromEntityAssembler.toResourceFromEntity(request, details);

      // Assert
      assertThat(resource.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.equipmentName()).isEqualTo("Freezer 1");
      assertThat(resource.siteName()).isEqualTo("Lima");
      assertThat(resource.technicianName()).isEqualTo("Luis Rojas");
      assertThat(resource.status()).isEqualTo(ServiceStatusResource.COMPLETED);
      assertThat(resource.completedAt()).isEqualTo(request.getCompletedAt().toString());
      assertThat(resource.intervention()).isNotNull();
      assertThat(resource.intervention().serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(resource.history()).hasSize(request.getHistory().size());
      assertThat(resource.maintenanceWindows()).hasSize(1);
      var window = resource.maintenanceWindows().get(0);
      assertThat(window.windowId()).isEqualTo(WINDOW_ID);
      assertThat(window.origin()).isEqualTo("APP");
      assertThat(window.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(window.endedAt()).isNull();
      assertThat(window.closureReason()).isNull();
    }

    @Test
    @DisplayName("toResourceFromEntity(canceled) -> cancel reason and instant exposed (AAA)")
    void canceledResource() {
      // Arrange
      var request = canceled();

      // Act
      var resource = ServiceRequestResourceFromEntityAssembler.toResourceFromEntity(request, details);

      // Assert
      assertThat(resource.status()).isEqualTo(ServiceStatusResource.CANCELED);
      assertThat(resource.cancelReason()).isEqualTo("No longer needed");
      assertThat(resource.canceledAt()).isEqualTo(request.getCanceledAt().toString());
      assertThat(resource.assignedAt()).isEqualTo(request.getAssignedAt().toString());
    }

    @Test
    @DisplayName("toResourceFromAccepted(accepted, details) -> request resource carrying the once-shown edge grant (AAA)")
    void acceptedResource() {
      // Arrange
      var accepted = new AcceptedServiceRequest(accepted(), new IssuedEdgeGrant(grant(), "it_grt_secret"));

      // Act
      var resource = ServiceRequestResourceFromEntityAssembler.toResourceFromAccepted(accepted, details);

      // Assert
      assertThat(resource.status()).isEqualTo(ServiceStatusResource.ACCEPTED);
      assertThat(resource.edgeGrant()).isNotNull();
      assertThat(resource.edgeGrant().token()).isEqualTo("it_grt_secret");
      assertThat(resource.technicianName()).isEqualTo("Luis Rojas");
    }

    @Test
    @DisplayName("toSummaryResourceFromEntity -> flags for intervention and review plus resolved names (AAA)")
    void summary() {
      // Arrange
      var request = completed();

      // Act
      var summary = ServiceRequestResourceFromEntityAssembler.toSummaryResourceFromEntity(request, details);
      var bare = ServiceRequestResourceFromEntityAssembler.toSummaryResourceFromEntity(pending(), null);

      // Assert
      assertThat(summary.id()).isEqualTo(REQUEST_ID);
      assertThat(summary.hasIntervention()).isTrue();
      assertThat(summary.hasReview()).isTrue();
      assertThat(summary.equipmentName()).isEqualTo("Freezer 1");
      assertThat(summary.siteName()).isEqualTo("Lima");
      assertThat(summary.technicianName()).isEqualTo("Luis Rojas");
      assertThat(summary.status()).isEqualTo(ServiceStatusResource.COMPLETED);
      assertThat(bare.hasIntervention()).isFalse();
      assertThat(bare.hasReview()).isFalse();
      assertThat(bare.technicianId()).isNull();
      assertThat(bare.equipmentName()).isNull();
    }

    @Test
    @DisplayName("toPagedResourceFromPage / toPagedSummaryResourceFromPage -> content mapped and paging copied (AAA)")
    void paged() {
      // Arrange
      var page = new ServiceRequestPage(List.of(completed(), assigned()), 2, 2, 5);

      // Act
      var full = ServiceRequestResourceFromEntityAssembler.toPagedResourceFromPage(page, details);
      var summaries = ServiceRequestResourceFromEntityAssembler.toPagedSummaryResourceFromPage(page, details);

      // Assert
      assertThat(full.content()).hasSize(2);
      assertThat(full.page()).isEqualTo(2);
      assertThat(full.size()).isEqualTo(2);
      assertThat(full.totalElements()).isEqualTo(5L);
      assertThat(full.totalPages()).isEqualTo(3);
      assertThat(summaries.content()).hasSize(2);
      assertThat(summaries.totalPages()).isEqualTo(3);
      assertThat(summaries.content().get(0).status()).isEqualTo(ServiceStatusResource.COMPLETED);
      assertThat(summaries.content().get(1).status()).isEqualTo(ServiceStatusResource.PENDING);
    }
  }
}
