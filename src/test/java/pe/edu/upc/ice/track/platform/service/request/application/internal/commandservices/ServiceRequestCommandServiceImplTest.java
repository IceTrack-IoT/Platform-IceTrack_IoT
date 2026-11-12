package pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices.ServiceRequestCommandServiceImpl;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.EquipmentRef;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalMonitoringService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.tokens.EdgeGrantTokenService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.EdgeGrantRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class ServiceRequestCommandServiceImplTest {

  private static final ServiceRequestId STORED_ID = new ServiceRequestId(REQUEST_ID);

  @Mock private ServiceRequestRepository requests;
  @Mock private ExternalEquipmentService equipment;
  @Mock private ExternalUserProfileService profiles;
  @Mock private ExternalMonitoringService monitoring;
  @Mock private EdgeGrantRepository grants;
  @Mock private EdgeGrantTokenService tokens;

  private ServiceRequestCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ServiceRequestCommandServiceImpl(requests, equipment, profiles, monitoring, grants, tokens, 24L);
  }

  private void asOwner() {
    doReturn(Optional.of(OWNER_ID)).when(profiles).fetchOwnerIdByUsername(USERNAME);
  }

  private void asTechnician() {
    doReturn(Optional.of(TECHNICIAN_ID)).when(profiles).fetchTechnicianIdByUsername(USERNAME);
  }

  private void stored(ServiceRequest request) {
    doReturn(Optional.of(request)).when(requests).findById(STORED_ID);
  }

  private void saveEchoes() {
    doAnswer(inv -> inv.getArgument(0)).when(requests).save(any(ServiceRequest.class));
  }

  private void grantSavesEcho() {
    doAnswer(inv -> inv.getArgument(0)).when(grants).save(any(EdgeGrant.class));
  }

  @Test
  @DisplayName("new ServiceRequestCommandServiceImpl(..., ttl < 1) -> IllegalStateException (AAA)")
  void invalidTtl() {
    // Act & Assert
    assertThatThrownBy(() -> new ServiceRequestCommandServiceImpl(requests, equipment, profiles, monitoring, grants, tokens, 0L))
        .isInstanceOf(IllegalStateException.class).hasMessage("service-request.edge-grant.ttl-hours must be at least 1");
  }

  @Nested
  class Create {

    @Test
    @DisplayName("handle(CreateServiceRequestCommand) -> persisted PENDING request on the owner's equipment and site (AAA)")
    void creates() {
      // Arrange
      asOwner();
      doReturn(Optional.of(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID))).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      doAnswer(inv -> withId(inv.getArgument(0))).when(requests).save(any(ServiceRequest.class));

      // Act
      var request = successValue(service.handle(createCommand(null)));

      // Assert
      assertThat(request.getId()).isEqualTo(STORED_ID);
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(request.getOwnerId().ownerId()).isEqualTo(OWNER_ID);
      assertThat(request.getSiteId().siteId()).isEqualTo(SITE_ID);
      assertThat(request.getEquipmentId().equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(request.getSourceAlertId()).isNull();
      verify(requests, times(1)).save(any(ServiceRequest.class));
      verifyNoInteractions(monitoring);
    }

    @Test
    @DisplayName("handle(CreateServiceRequestCommand) for an account without owner profile -> ACCESS_DENIED (AAA)")
    void noOwnerProfile() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername(USERNAME);

      // Act
      var error = failureError(service.handle(createCommand(null)));

      // Assert
      assertThat(error.code()).isEqualTo("ACCESS_DENIED");
      assertThat(error.details()).isEqualTo("The authenticated account has no owner profile, so it owns no service request");
      verifyNoInteractions(requests, equipment, monitoring);
    }

    @Test
    @DisplayName("handle(CreateServiceRequestCommand) for equipment the owner cannot reach -> EQUIPMENT_NOT_FOUND (AAA)")
    void equipmentNotReachable() {
      // Arrange
      asOwner();
      doReturn(Optional.empty()).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);

      // Act
      var error = failureError(service.handle(createCommand(null)));

      // Assert
      assertThat(error.code()).isEqualTo("EQUIPMENT_NOT_FOUND");
      assertThat(error.message()).isEqualTo("Equipment not found: 10");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(command with source alert of the same unit) -> request keeps the alert id (AAA)")
    void withSourceAlert() {
      // Arrange
      asOwner();
      doReturn(Optional.of(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID))).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      doReturn(Optional.of(EQUIPMENT_ID)).when(monitoring).fetchAlertEquipmentId(ALERT_ID);
      doReturn(true).when(equipment).isEquipmentOwnedBy(OWNER_ID, EQUIPMENT_ID);
      doAnswer(inv -> withId(inv.getArgument(0))).when(requests).save(any(ServiceRequest.class));

      // Act
      var request = successValue(service.handle(createCommand(ALERT_ID)));

      // Assert
      assertThat(request.getSourceAlertId()).isEqualTo(ALERT_ID);
    }

    @Test
    @DisplayName("handle(command with unknown alert or alert of a unit not owned) -> ALERT_NOT_FOUND (AAA)")
    void alertNotReachable() {
      // Arrange
      asOwner();
      doReturn(Optional.of(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID))).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      doReturn(Optional.empty()).when(monitoring).fetchAlertEquipmentId(ALERT_ID);
      doReturn(Optional.of(99L)).when(monitoring).fetchAlertEquipmentId(56L);
      doReturn(false).when(equipment).isEquipmentOwnedBy(OWNER_ID, 99L);

      // Act
      var unknown = failureError(service.handle(createCommand(ALERT_ID)));
      var foreign = failureError(service.handle(createCommand(56L)));

      // Assert
      assertThat(unknown.code()).isEqualTo("ALERT_NOT_FOUND");
      assertThat(unknown.message()).isEqualTo("Alert not found: 55");
      assertThat(foreign.code()).isEqualTo("ALERT_NOT_FOUND");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(command with alert of another owned unit) -> VALIDATION_ERROR on source_alert_id (AAA)")
    void alertOfAnotherUnit() {
      // Arrange
      asOwner();
      doReturn(Optional.of(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID))).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      doReturn(Optional.of(99L)).when(monitoring).fetchAlertEquipmentId(ALERT_ID);
      doReturn(true).when(equipment).isEquipmentOwnedBy(OWNER_ID, 99L);

      // Act
      var error = failureError(service.handle(createCommand(ALERT_ID)));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.message()).isEqualTo("Validation failed: source_alert_id");
      assertThat(error.details()).isEqualTo("Field source_alert_id: belongs to another unit");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(command with blank description) -> VALIDATION_ERROR carrying the domain message, nothing saved (AAA)")
    void domainValidation() {
      // Arrange
      asOwner();
      doReturn(Optional.of(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID))).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      var command = new CreateServiceRequestCommand(USERNAME, EQUIPMENT_ID, pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType.REPAIR,
          pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority.LOW, "  ", null);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("Service request description must not be null or blank");
      verify(requests, never()).save(any());
    }
  }

  @Nested
  class Assign {

    private final AssignTechnicianCommand command = new AssignTechnicianCommand(REQUEST_ID, TECHNICIAN_ID, USERNAME);

    @Test
    @DisplayName("handle(AssignTechnicianCommand) -> technician set, saved, and previous edge grants revoked as REASSIGNED (AAA)")
    void assigns() {
      // Arrange
      asOwner();
      stored(pending());
      saveEchoes();
      grantSavesEcho();
      var oldGrant = grant();
      doReturn(List.of(oldGrant)).when(grants).findUnrevokedByServiceRequestId(REQUEST_ID);
      doReturn(true).when(profiles).isTechnicianEligibleForAssignment(TECHNICIAN_ID);

      // Act
      var request = successValue(service.handle(command));

      // Assert
      assertThat(request.getTechnicianId()).isEqualTo(technician());
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(oldGrant.isRevoked()).isTrue();
      assertThat(oldGrant.getRevocationReason()).isEqualTo("REASSIGNED");
      verify(grants, times(1)).save(oldGrant);
    }

    @Test
    @DisplayName("handle(AssignTechnicianCommand) for a missing or foreign request -> SERVICE_REQUEST_NOT_FOUND (AAA)")
    void notFound() {
      // Arrange
      asOwner();
      doReturn(Optional.empty()).when(requests).findById(STORED_ID);
      doReturn(Optional.of(pending())).when(requests).findById(new ServiceRequestId(78L));
      doReturn(Optional.of(OTHER_OWNER_ID)).when(profiles).fetchOwnerIdByUsername("other");

      // Act
      var missing = failureError(service.handle(command));
      var invalidId = failureError(service.handle(new AssignTechnicianCommand(0L, TECHNICIAN_ID, USERNAME)));
      var nullId = failureError(service.handle(new AssignTechnicianCommand(null, TECHNICIAN_ID, USERNAME)));
      var foreign = failureError(service.handle(new AssignTechnicianCommand(78L, TECHNICIAN_ID, "other")));

      // Assert
      assertThat(missing.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(missing.message()).isEqualTo("Service_Request not found: 77");
      assertThat(invalidId.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(nullId.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(foreign.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(AssignTechnicianCommand) for a technician not ready -> TECHNICIAN_NOT_ELIGIBLE, nothing saved (AAA)")
    void notEligible() {
      // Arrange
      asOwner();
      stored(pending());
      doReturn(false).when(profiles).isTechnicianEligibleForAssignment(TECHNICIAN_ID);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("TECHNICIAN_NOT_ELIGIBLE");
      assertThat(error.message()).isEqualTo("Technician not eligible for assignment");
      assertThat(error.details()).isEqualTo("Technician 7 is not ready to be assigned a service request");
      verify(requests, never()).save(any());
      verifyNoInteractions(grants);
    }

    @Test
    @DisplayName("handle(AssignTechnicianCommand) on a COMPLETED request -> INVALID_STATUS_TRANSITION with the domain reason (AAA)")
    void invalidStatus() {
      // Arrange
      asOwner();
      stored(completed());
      doReturn(true).when(profiles).isTechnicianEligibleForAssignment(TECHNICIAN_ID);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(error.message()).isEqualTo("Invalid service request status transition");
      assertThat(error.details()).startsWith("Cannot assign a technician to a service request in status COMPLETED");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(AssignTechnicianCommand) for an account without owner profile -> ACCESS_DENIED (AAA)")
    void noOwner() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername(USERNAME);

      // Act & Assert
      assertThat(failureError(service.handle(command)).code()).isEqualTo("ACCESS_DENIED");
      verifyNoInteractions(requests);
    }
  }

  @Nested
  class AcceptAndRegenerate {

    @Test
    @DisplayName("handle(AcceptServiceRequestCommand) -> ACCEPTED, saved, and a fresh edge grant issued for 24 h with the token (AAA)")
    void accepts() {
      // Arrange
      asTechnician();
      stored(assigned());
      saveEchoes();
      grantSavesEcho();
      var previous = grant();
      doReturn(List.of(previous)).when(grants).findUnrevokedByServiceRequestId(REQUEST_ID);
      doReturn(new EdgeGrantTokenService.IssuedEdgeGrantToken("it_grt_plain", DIGEST)).when(tokens).generate();
      var before = Instant.now();

      // Act
      var accepted = successValue(service.handle(new AcceptServiceRequestCommand(REQUEST_ID, USERNAME)));

      // Assert
      assertThat(accepted.serviceRequest().getStatus()).isEqualTo(ServiceStatus.ACCEPTED);
      assertThat(accepted.edgeGrant().token()).isEqualTo("it_grt_plain");
      var issued = accepted.edgeGrant().grant();
      assertThat(issued.getTokenDigest()).isEqualTo(DIGEST);
      assertThat(issued.getServiceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(issued.getTechnicianProfileId()).isEqualTo(TECHNICIAN_ID);
      assertThat(issued.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(Duration.between(issued.getIssuedAt(), issued.getExpiresAt())).isEqualTo(Duration.ofHours(24));
      assertThat(issued.getIssuedAt()).isAfterOrEqualTo(before.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
      assertThat(previous.getRevocationReason()).isEqualTo("REGENERATED");
    }

    @Test
    @DisplayName("handle(AcceptServiceRequestCommand) guards -> no technician profile, not assigned, wrong status (AAA)")
    void acceptGuards() {
      // Arrange
      var command = new AcceptServiceRequestCommand(REQUEST_ID, USERNAME);
      doReturn(Optional.empty()).when(profiles).fetchTechnicianIdByUsername("nobody");
      asTechnician();
      doReturn(Optional.of(pending())).when(requests).findById(STORED_ID);

      // Act
      var noProfile = failureError(service.handle(new AcceptServiceRequestCommand(REQUEST_ID, "nobody")));
      var notAssigned = failureError(service.handle(command));
      doReturn(Optional.of(accepted())).when(requests).findById(STORED_ID);
      var wrongStatus = failureError(service.handle(command));

      // Assert
      assertThat(noProfile.code()).isEqualTo("ACCESS_DENIED");
      assertThat(noProfile.details()).isEqualTo("The authenticated account has no technician profile, so no service request is assigned to it");
      assertThat(notAssigned.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(wrongStatus.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      verify(requests, never()).save(any());
      verifyNoInteractions(tokens, grants);
    }

    @Test
    @DisplayName("handle(RegenerateEdgeGrantCommand) on ACCEPTED / IN_PROGRESS -> a new grant, request untouched (AAA)")
    void regenerates() {
      // Arrange
      asTechnician();
      grantSavesEcho();
      doReturn(new EdgeGrantTokenService.IssuedEdgeGrantToken("it_grt_new", DIGEST)).when(tokens).generate();
      var command = new RegenerateEdgeGrantCommand(REQUEST_ID, USERNAME);

      // Act
      stored(accepted());
      var fromAccepted = successValue(service.handle(command));
      stored(inProgress());
      var fromInProgress = successValue(service.handle(command));

      // Assert
      assertThat(fromAccepted.token()).isEqualTo("it_grt_new");
      assertThat(fromInProgress.grant().getTechnicianProfileId()).isEqualTo(TECHNICIAN_ID);
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(RegenerateEdgeGrantCommand) guards -> missing, other technician, wrong status (AAA)")
    void regenerateGuards() {
      // Arrange
      asTechnician();
      var command = new RegenerateEdgeGrantCommand(REQUEST_ID, USERNAME);
      doReturn(Optional.empty()).when(requests).findById(STORED_ID);

      // Act
      var missing = failureError(service.handle(command));
      doReturn(Optional.of(pending())).when(requests).findById(STORED_ID);
      var otherTechnician = failureError(service.handle(command));
      doReturn(Optional.of(assigned())).when(requests).findById(STORED_ID);
      var pendingStatus = failureError(service.handle(command));
      doReturn(Optional.of(completed())).when(requests).findById(STORED_ID);
      var completedStatus = failureError(service.handle(command));

      // Assert
      assertThat(missing.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(otherTechnician.code()).isEqualTo("ACCESS_DENIED");
      assertThat(otherTechnician.details()).isEqualTo("Only the technician assigned to the service request may regenerate its edge grant");
      assertThat(pendingStatus.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(pendingStatus.details())
          .isEqualTo("An edge grant can only be regenerated for an ACCEPTED or IN_PROGRESS service request, not PENDING");
      assertThat(completedStatus.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      verifyNoInteractions(tokens, grants);
    }

    @Test
    @DisplayName("handle(RegenerateEdgeGrantCommand) without technician profile -> ACCESS_DENIED (AAA)")
    void regenerateNoProfile() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchTechnicianIdByUsername(USERNAME);

      // Act & Assert
      assertThat(failureError(service.handle(new RegenerateEdgeGrantCommand(REQUEST_ID, USERNAME))).code()).isEqualTo("ACCESS_DENIED");
      verifyNoInteractions(requests);
    }
  }

  @Nested
  class RejectAndStart {

    @Test
    @DisplayName("handle(RejectServiceRequestCommand) -> technician cleared and request saved as PENDING (AAA)")
    void rejects() {
      // Arrange
      asTechnician();
      stored(assigned());
      saveEchoes();

      // Act
      var request = successValue(service.handle(new RejectServiceRequestCommand(REQUEST_ID, USERNAME, "Too far")));

      // Assert
      assertThat(request.getTechnicianId()).isNull();
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.PENDING);
      verify(requests, times(1)).save(request);
    }

    @Test
    @DisplayName("handle(RejectServiceRequestCommand) with blank reason -> VALIDATION_ERROR; by another technician -> NOT_FOUND (AAA)")
    void rejectGuards() {
      // Arrange
      asTechnician();
      stored(assigned());
      doReturn(Optional.of(8L)).when(profiles).fetchTechnicianIdByUsername("other");

      // Act
      var blank = failureError(service.handle(new RejectServiceRequestCommand(REQUEST_ID, USERNAME, " ")));
      var other = failureError(service.handle(new RejectServiceRequestCommand(REQUEST_ID, "other", "x")));

      // Assert
      assertThat(blank.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(blank.details()).isEqualTo("A rejection reason is required");
      assertThat(other.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(StartServiceRequestCommand) -> IN_PROGRESS with the device start time, saved (AAA)")
    void starts() {
      // Arrange
      asTechnician();
      stored(accepted());
      saveEchoes();
      var device = Instant.now().minusSeconds(300);

      // Act
      var request = successValue(service.handle(new StartServiceRequestCommand(REQUEST_ID, USERNAME, device)));

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.IN_PROGRESS);
      assertThat(request.getStartedAt()).isEqualTo(device.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("handle(StartServiceRequestCommand) on a PENDING request / device time in the future -> error results (AAA)")
    void startGuards() {
      // Arrange
      asTechnician();
      doReturn(Optional.of(assigned())).when(requests).findById(STORED_ID);

      // Act
      var pendingStatus = failureError(service.handle(new StartServiceRequestCommand(REQUEST_ID, USERNAME, null)));
      doReturn(Optional.of(accepted())).when(requests).findById(STORED_ID);
      var future = failureError(service.handle(new StartServiceRequestCommand(REQUEST_ID, USERNAME, Instant.now().plus(Duration.ofHours(1)))));

      // Assert
      assertThat(pendingStatus.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(future.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(future.details()).isEqualTo("Service request startedAt must not lie in the future");
      verify(requests, never()).save(any());
    }
  }

  @Nested
  class RegisterIntervention {

    @Test
    @DisplayName("handle(RegisterInterventionCommand) -> COMPLETED, saved, edge grants revoked as COMPLETED, not a replay (AAA)")
    void registers() {
      // Arrange
      asTechnician();
      stored(inProgress());
      saveEchoes();
      grantSavesEcho();
      var open = grant();
      doReturn(List.of(open)).when(grants).findUnrevokedByServiceRequestId(REQUEST_ID);
      doReturn(false).when(requests).existsByInterventionClientGeneratedId(CLIENT_ID);

      // Act
      var registration = successValue(service.handle(interventionCommand(CLIENT_ID)));

      // Assert
      assertThat(registration.replayed()).isFalse();
      assertThat(registration.serviceRequest().getStatus()).isEqualTo(ServiceStatus.COMPLETED);
      assertThat(registration.intervention().getDiagnosis()).isEqualTo("Worn bearing");
      assertThat(open.getRevocationReason()).isEqualTo("COMPLETED");
      verify(requests, times(1)).save(registration.serviceRequest());
    }

    @Test
    @DisplayName("handle(RegisterInterventionCommand) replayed -> existing intervention, no save, no grant revocation, no global lookup (AAA)")
    void replay() {
      // Arrange
      asTechnician();
      stored(completed());

      // Act
      var registration = successValue(service.handle(interventionCommand(CLIENT_ID)));

      // Assert
      assertThat(registration.replayed()).isTrue();
      verify(requests, never()).save(any());
      verify(requests, never()).existsByInterventionClientGeneratedId(any());
      verifyNoInteractions(grants);
    }

    @Test
    @DisplayName("handle(RegisterInterventionCommand) whose client id belongs to another request -> DUPLICATE_RESOURCE_CONFLICT (AAA)")
    void duplicateAcrossRequests() {
      // Arrange
      asTechnician();
      stored(inProgress());
      doReturn(true).when(requests).existsByInterventionClientGeneratedId(CLIENT_ID);

      // Act
      var error = failureError(service.handle(interventionCommand(CLIENT_ID)));

      // Assert
      assertThat(error.code()).isEqualTo("DUPLICATE_RESOURCE_CONFLICT");
      assertThat(error.details()).isEqualTo(
          "client_generated_id 11111111-1111-1111-1111-111111111111 already identifies the intervention of another service request");
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(RegisterInterventionCommand) without client id -> VALIDATION_ERROR, global lookup skipped (AAA)")
    void missingClientId() {
      // Arrange
      asTechnician();
      stored(inProgress());

      // Act
      var error = failureError(service.handle(interventionCommand(null)));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("Service request clientGeneratedId must not be null");
      verify(requests, never()).existsByInterventionClientGeneratedId(any());
    }

    @Test
    @DisplayName("handle(RegisterInterventionCommand) on a request not IN_PROGRESS or by another technician -> error results (AAA)")
    void guards() {
      // Arrange
      asTechnician();
      doReturn(false).when(requests).existsByInterventionClientGeneratedId(CLIENT_ID);
      doReturn(Optional.of(accepted())).when(requests).findById(STORED_ID);

      // Act
      var status = failureError(service.handle(interventionCommand(CLIENT_ID)));
      doReturn(Optional.of(pending())).when(requests).findById(STORED_ID);
      var notAssigned = failureError(service.handle(interventionCommand(CLIENT_ID)));

      // Assert
      assertThat(status.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(notAssigned.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      verify(requests, never()).save(any());
    }
  }

  @Nested
  class Cancel {

    @Test
    @DisplayName("handle(CancelServiceRequestCommand) -> CANCELED, saved, edge grants revoked as CANCELED (AAA)")
    void cancels() {
      // Arrange
      asOwner();
      stored(accepted());
      saveEchoes();
      grantSavesEcho();
      var open = grant();
      doReturn(List.of(open)).when(grants).findUnrevokedByServiceRequestId(REQUEST_ID);

      // Act
      var request = successValue(service.handle(new CancelServiceRequestCommand(REQUEST_ID, USERNAME, "Changed plans")));

      // Assert
      assertThat(request.getStatus()).isEqualTo(ServiceStatus.CANCELED);
      assertThat(request.getCancelReason()).isEqualTo("Changed plans");
      assertThat(open.getRevocationReason()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("handle(CancelServiceRequestCommand) guards -> blank reason, IN_PROGRESS, foreign owner (AAA)")
    void guards() {
      // Arrange
      asOwner();
      doReturn(Optional.of(pending())).when(requests).findById(STORED_ID);
      var blank = failureError(service.handle(new CancelServiceRequestCommand(REQUEST_ID, USERNAME, " ")));
      doReturn(Optional.of(inProgress())).when(requests).findById(STORED_ID);
      var running = failureError(service.handle(new CancelServiceRequestCommand(REQUEST_ID, USERNAME, "x")));
      doReturn(Optional.of(OTHER_OWNER_ID)).when(profiles).fetchOwnerIdByUsername("other");
      var foreign = failureError(service.handle(new CancelServiceRequestCommand(REQUEST_ID, "other", "x")));

      // Assert
      assertThat(blank.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(running.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(foreign.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      verify(requests, never()).save(any());
      verifyNoInteractions(grants);
    }
  }

  @Nested
  class MaintenanceWindows {

    private RecordMaintenanceWindowLinkCommand record(Instant attributedAt, Long equipmentId) {
      return new RecordMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, equipmentId, MaintenanceOrigin.APP, TECHNICIAN_ID,
          attributedAt, attributedAt);
    }

    @Test
    @DisplayName("handle(RecordMaintenanceWindowLinkCommand) for an IN_PROGRESS request of the same unit -> true and saved (AAA)")
    void links() {
      // Arrange
      var running = inProgress();
      stored(running);
      saveEchoes();

      // Act
      var linked = service.handle(record(running.getStartedAt().plusSeconds(5), EQUIPMENT_ID));

      // Assert
      assertThat(linked).isTrue();
      assertThat(running.getMaintenanceWindows()).hasSize(1);
      verify(requests, times(1)).save(running);
    }

    @Test
    @DisplayName("handle(RecordMaintenanceWindowLinkCommand) missing request / other unit -> false, nothing saved (AAA)")
    void notLinked() {
      // Arrange
      doReturn(Optional.empty()).when(requests).findById(STORED_ID);

      // Act
      var missing = service.handle(record(now(), EQUIPMENT_ID));
      stored(inProgress());
      var otherUnit = service.handle(record(now(), 999L));

      // Assert
      assertThat(missing).isFalse();
      assertThat(otherUnit).isFalse();
      verify(requests, never()).save(any());
    }

    @Test
    @DisplayName("handle(RecordMaintenanceWindowLinkCommand) when not IN_PROGRESS at the attributed instant or already linked -> false (AAA)")
    void rejectedLinks() {
      // Arrange
      var running = inProgress();
      stored(running);
      saveEchoes();
      var before = running.getStartedAt().minusSeconds(1);
      var inside = running.getStartedAt().plusSeconds(5);

      // Act
      var tooEarly = service.handle(record(before, EQUIPMENT_ID));
      var first = service.handle(record(inside, EQUIPMENT_ID));
      var duplicate = service.handle(record(inside, EQUIPMENT_ID));

      // Assert
      assertThat(tooEarly).isFalse();
      assertThat(first).isTrue();
      assertThat(duplicate).isFalse();
      verify(requests, times(1)).save(running);
    }

    @Test
    @DisplayName("handle(CompleteMaintenanceWindowLinkCommand) for an open link -> true and saved (AAA)")
    void completes() {
      // Arrange
      var running = inProgress();
      var at = running.getStartedAt().plusSeconds(5);
      running.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, at, at);
      stored(running);
      saveEchoes();

      // Act
      var completed = service.handle(new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, at.plusSeconds(60), "COMPLETED"));

      // Assert
      assertThat(completed).isTrue();
      assertThat(running.getMaintenanceWindows().get(0).getEndedAt()).isEqualTo(at.plusSeconds(60));
      verify(requests, times(1)).save(running);
    }

    @Test
    @DisplayName("handle(CompleteMaintenanceWindowLinkCommand) missing request / no open link / invalid reason -> false (AAA)")
    void notCompleted() {
      // Arrange
      var running = inProgress();
      var at = running.getStartedAt().plusSeconds(5);
      running.linkMaintenanceWindow(WINDOW_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, at, at);
      doReturn(Optional.empty()).when(requests).findById(new ServiceRequestId(78L));
      stored(running);

      // Act
      var missing = service.handle(new CompleteMaintenanceWindowLinkCommand(78L, WINDOW_ID, at, "DONE"));
      var unknownWindow = service.handle(new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, 999L, at, "DONE"));
      var reasonTooLong = service.handle(new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, at, "x".repeat(31)));

      // Assert
      assertThat(missing).isFalse();
      assertThat(unknownWindow).isFalse();
      assertThat(reasonTooLong).isFalse();
      verify(requests, never()).save(any());
    }
  }

  @Test
  @DisplayName("grant TTL -> edge grants expire after the configured number of hours (AAA)")
  void customTtl() {
    // Arrange
    var custom = new ServiceRequestCommandServiceImpl(requests, equipment, profiles, monitoring, grants, tokens, 2L);
    asTechnician();
    stored(accepted());
    grantSavesEcho();
    doReturn(new EdgeGrantTokenService.IssuedEdgeGrantToken("it_grt_x", DIGEST)).when(tokens).generate();

    // Act
    var issued = successValue(custom.handle(new RegenerateEdgeGrantCommand(REQUEST_ID, USERNAME)));

    // Assert
    assertThat(Duration.between(issued.grant().getIssuedAt(), issued.grant().getExpiresAt())).isEqualTo(Duration.ofHours(2));
  }
}
