package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentSummary;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceInterventionRecord;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

class ReportingAclServicesTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  class Assets {

    @Mock private AssetContextFacade facade;
    @InjectMocks private ExternalAssetsServiceForReporting service;

    @Test
    @DisplayName("fetchEquipmentIdsBySite(siteId) -> delegates to the facade (AAA)")
    void equipmentIds() {
      // Arrange
      doReturn(List.of(1L, 2L)).when(facade).fetchEquipmentIdsBySite(SITE_ID);

      // Act & Assert
      assertThat(service.fetchEquipmentIdsBySite(SITE_ID)).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("isSiteOwnedBy / isEquipmentOwnedBy -> delegate to the existence checks (AAA)")
    void ownership() {
      // Arrange
      doReturn(true).when(facade).existsSiteForOwner(SITE_ID, OWNER_ID);
      doReturn(false).when(facade).existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID);

      // Act & Assert
      assertThat(service.isSiteOwnedBy(SITE_ID, OWNER_ID)).isTrue();
      assertThat(service.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_ID)).isFalse();
    }

    @Test
    @DisplayName("fetchTemperatureThresholdRange(id) -> returns the facade's range (AAA)")
    void thresholds() {
      // Arrange
      var range = new double[] {-20.0, -10.0};
      doReturn(Optional.of(range)).when(facade).getEquipmentThresholdRange(EQUIPMENT_ID);

      // Act & Assert
      assertThat(service.fetchTemperatureThresholdRange(EQUIPMENT_ID)).containsSame(range);
    }

    @Test
    @DisplayName("fetchEquipmentAndSiteNames(id) -> [name, siteName] from the summary, empty when unknown (AAA)")
    void names() {
      // Arrange
      doReturn(Optional.of(new EquipmentSummary(EQUIPMENT_ID, "Freezer 1", "ACTIVE", SITE_ID, "Lima Norte", Instant.now())))
          .when(facade).fetchEquipmentSummary(EQUIPMENT_ID);
      doReturn(Optional.empty()).when(facade).fetchEquipmentSummary(99L);

      // Act & Assert
      assertThat(service.fetchEquipmentAndSiteNames(EQUIPMENT_ID)).hasValueSatisfying(
          names -> assertThat(names).containsExactly("Freezer 1", "Lima Norte"));
      assertThat(service.fetchEquipmentAndSiteNames(99L)).isEmpty();
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class Monitoring {

    @Mock private MonitoringContextFacade facade;
    @InjectMocks private ExternalMonitoringServiceForReporting service;

    @Test
    @DisplayName("count / duration / samples queries -> delegate with the same arguments (AAA)")
    void delegates() {
      // Arrange
      var samples = List.of(new double[] {-18.0, 1.0});
      doReturn(3L).when(facade).countAlertsByEquipmentAndTypeInRange(EQUIPMENT_ID, "DEVICE_OFFLINE", FROM, TO);
      doReturn(1.5).when(facade).fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(EQUIPMENT_ID, "DEVICE_OFFLINE", FROM, TO);
      doReturn(samples).when(facade).fetchTemperatureSamplesByEquipmentInRange(EQUIPMENT_ID, FROM, TO);

      // Act & Assert
      assertThat(service.countAlertsByEquipmentAndTypeInRange(EQUIPMENT_ID, "DEVICE_OFFLINE", FROM, TO)).isEqualTo(3L);
      assertThat(service.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(EQUIPMENT_ID, "DEVICE_OFFLINE", FROM, TO)).isEqualTo(1.5);
      assertThat(service.fetchTemperatureSamplesByEquipmentInRange(EQUIPMENT_ID, FROM, TO)).isSameAs(samples);
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class Profiles {

    @Mock private IamContextFacade iam;
    @Mock private ProfilesContextFacade profiles;
    @InjectMocks private ExternalProfilesServiceForReporting service;

    @Test
    @DisplayName("fetchUserIdByUsername(valid) -> the id the IAM facade resolves (AAA)")
    void userId() {
      // Arrange
      doReturn(USER_ID).when(iam).fetchUserIdByUsername(USERNAME);

      // Act & Assert
      assertThat(service.fetchUserIdByUsername(USERNAME)).contains(USER_ID);
    }

    @Test
    @DisplayName("fetchUserIdByUsername(null / blank) -> empty without calling IAM (AAA)")
    void userIdBlank() {
      // Act & Assert
      assertThat(service.fetchUserIdByUsername(null)).isEmpty();
      assertThat(service.fetchUserIdByUsername("   ")).isEmpty();
      verifyNoInteractions(iam);
    }

    @Test
    @DisplayName("fetchUserIdByUsername(unknown) -> empty when IAM answers 0 or null (AAA)")
    void userIdNotFound() {
      // Arrange
      doReturn(0L).when(iam).fetchUserIdByUsername("ghost");
      doReturn(null).when(iam).fetchUserIdByUsername("nobody");

      // Act & Assert
      assertThat(service.fetchUserIdByUsername("ghost")).isEmpty();
      assertThat(service.fetchUserIdByUsername("nobody")).isEmpty();
    }

    @Test
    @DisplayName("fetchOwnerIdByUserId -> id when found; empty for null, 0 or null answer (AAA)")
    void ownerId() {
      // Arrange
      doReturn(OWNER_ID).when(profiles).fetchOwnerIdByUserId(USER_ID);
      doReturn(0L).when(profiles).fetchOwnerIdByUserId(OTHER_USER_ID);
      doReturn(null).when(profiles).fetchOwnerIdByUserId(11L);

      // Act & Assert
      assertThat(service.fetchOwnerIdByUserId(USER_ID)).contains(OWNER_ID);
      assertThat(service.fetchOwnerIdByUserId(OTHER_USER_ID)).isEmpty();
      assertThat(service.fetchOwnerIdByUserId(11L)).isEmpty();
      assertThat(service.fetchOwnerIdByUserId(null)).isEmpty();
      verify(profiles, times(3)).fetchOwnerIdByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("fetchTechnicianFullName(id) -> name from the map, empty when absent or id null (AAA)")
    void technicianName() {
      // Arrange
      doReturn(Map.of(TECHNICIAN_ID, "Luis Rojas")).when(profiles).fetchTechnicianFullNames(List.of(TECHNICIAN_ID));
      doReturn(Map.of()).when(profiles).fetchTechnicianFullNames(List.of(99L));

      // Act & Assert
      assertThat(service.fetchTechnicianFullName(TECHNICIAN_ID)).contains("Luis Rojas");
      assertThat(service.fetchTechnicianFullName(99L)).isEmpty();
      assertThat(service.fetchTechnicianFullName(null)).isEmpty();
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class ServiceRequests {

    @Mock private ServiceRequestContextFacade facade;
    @InjectMocks private ExternalServiceRequestsServiceForReporting service;

    @Test
    @DisplayName("preventive maintenance ids / completed count -> delegate with the window unchanged (AAA)")
    void delegates() {
      // Arrange
      doReturn(List.of(1L)).when(facade).fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, TO);
      doReturn(4L).when(facade).countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, FROM, TO);

      // Act & Assert
      assertThat(service.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, TO)).containsExactly(1L);
      assertThat(service.countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, FROM, TO)).isEqualTo(4L);
    }

    @Test
    @DisplayName("fetchAverageRatingByTechnician(id) -> Optional of the rating, empty when never reviewed (AAA)")
    void rating() {
      // Arrange
      doReturn(4.5).when(facade).fetchAverageRatingByTechnician(TECHNICIAN_ID);
      doReturn(null).when(facade).fetchAverageRatingByTechnician(99L);

      // Act & Assert
      assertThat(service.fetchAverageRatingByTechnician(TECHNICIAN_ID)).contains(4.5);
      assertThat(service.fetchAverageRatingByTechnician(99L)).isEmpty();
    }

    @Test
    @DisplayName("fetchServiceIntervention(id) -> maps every field of the record, including history, work and rating (AAA)")
    void mapsFullRecord() {
      // Arrange
      var record = new ServiceInterventionRecord(REQUEST_ID, OWNER_ID, EQUIPMENT_ID, SITE_ID, TECHNICIAN_ID, "CORRECTIVE",
          "HIGH", "COMPLETED", "Noise", CREATED, STARTED, COMPLETED,
          List.of(new ServiceInterventionRecord.StatusChange("PENDING", "COMPLETED", COMPLETED, "ok")),
          new ServiceInterventionRecord.InterventionDetails("diag", "actions", List.of("part"), List.of("photo"), STARTED, COMPLETED),
          new ServiceInterventionRecord.ReviewDetails(5, 4, 3, 4.0, "nice", COMPLETED));
      doReturn(Optional.of(record)).when(facade).fetchServiceInterventionRecord(REQUEST_ID);

      // Act
      var result = service.fetchServiceIntervention(REQUEST_ID).orElseThrow();

      // Assert
      assertThat(result.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(result.ownerProfileId()).isEqualTo(OWNER_ID);
      assertThat(result.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(result.siteId()).isEqualTo(SITE_ID);
      assertThat(result.technicianProfileId()).isEqualTo(TECHNICIAN_ID);
      assertThat(result.type()).isEqualTo("CORRECTIVE");
      assertThat(result.priority()).isEqualTo("HIGH");
      assertThat(result.description()).isEqualTo("Noise");
      assertThat(result.isCompleted()).isTrue();
      assertThat(result.history()).hasSize(1);
      var change = result.history().get(0);
      assertThat(change.fromStatus()).isEqualTo("PENDING");
      assertThat(change.toStatus()).isEqualTo("COMPLETED");
      assertThat(change.reason()).isEqualTo("ok");
      assertThat(result.work().diagnosis()).isEqualTo("diag");
      assertThat(result.work().spareParts()).containsExactly("part");
      assertThat(result.work().photoEvidence()).containsExactly("photo");
      assertThat(result.rating().average()).isEqualTo(4.0);
      assertThat(result.rating().comment()).isEqualTo("nice");
    }

    @Test
    @DisplayName("fetchServiceIntervention(id) -> null intervention and review map to null, unknown id to empty (AAA)")
    void mapsSparseRecord() {
      // Arrange
      var record = new ServiceInterventionRecord(REQUEST_ID, OWNER_ID, EQUIPMENT_ID, SITE_ID, null, "CORRECTIVE",
          "LOW", "PENDING", "d", CREATED, null, null, null, null, null);
      doReturn(Optional.of(record)).when(facade).fetchServiceInterventionRecord(REQUEST_ID);
      doReturn(Optional.empty()).when(facade).fetchServiceInterventionRecord(99L);

      // Act
      var result = service.fetchServiceIntervention(REQUEST_ID).orElseThrow();

      // Assert
      assertThat(result.work()).isNull();
      assertThat(result.rating()).isNull();
      assertThat(result.history()).isEmpty();
      assertThat(result.isCompleted()).isFalse();
      assertThat(service.fetchServiceIntervention(99L)).isEmpty();
    }

    @Test
    @DisplayName("periodStart() / periodEnd() -> fall back work.startedAt > startedAt > createdAt and completedAt > work.completedAt > start (AAA)")
    void periods() {
      // Arrange
      var work = new ExternalServiceRequestsServiceForReporting.ServiceIntervention.Work("d", "a", List.of(), List.of(), STARTED.plusSeconds(5), COMPLETED.minusSeconds(5));
      var full = new ExternalServiceRequestsServiceForReporting.ServiceIntervention(1L, 1L, 1L, 1L, null, "t", "p", "COMPLETED", "d",
          CREATED, STARTED, COMPLETED, List.of(), work, null);
      var noWork = new ExternalServiceRequestsServiceForReporting.ServiceIntervention(1L, 1L, 1L, 1L, null, "t", "p", "COMPLETED", "d",
          CREATED, STARTED, null, List.of(), null, null);
      var onlyCreated = new ExternalServiceRequestsServiceForReporting.ServiceIntervention(1L, 1L, 1L, 1L, null, "t", "p", "COMPLETED", "d",
          CREATED, null, null, List.of(), null, null);
      var workEnd = new ExternalServiceRequestsServiceForReporting.ServiceIntervention(1L, 1L, 1L, 1L, null, "t", "p", "COMPLETED", "d",
          CREATED, null, null, List.of(), work, null);

      // Act & Assert
      assertThat(full.periodStart()).isEqualTo(STARTED.plusSeconds(5));
      assertThat(full.periodEnd()).isEqualTo(COMPLETED);
      assertThat(noWork.periodStart()).isEqualTo(STARTED);
      assertThat(noWork.periodEnd()).isEqualTo(STARTED);
      assertThat(onlyCreated.periodStart()).isEqualTo(CREATED);
      assertThat(onlyCreated.periodEnd()).isEqualTo(CREATED);
      assertThat(workEnd.periodEnd()).isEqualTo(COMPLETED.minusSeconds(5));
    }
  }
}
