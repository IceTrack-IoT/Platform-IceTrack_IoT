package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.*;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetDashboardSummaryQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryQueryServiceImplTest {

  private static final Long NEVER_REPORTED_ID = 9L;
  private static final Long SILENT_ID = 10L;

  @Mock
  private AlertRepository alertRepository;
  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private ExternalProfilesServiceForMonitoring externalProfilesService;
  @Mock
  private ExternalAssetManagementService externalAssetManagementService;
  @Mock
  private ExternalServiceRequestServiceForMonitoring externalServiceRequestService;
  @Mock
  private ExternalDeviceServiceForMonitoring externalDeviceService;

  @InjectMocks
  private DashboardSummaryQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetDashboardSummaryQuery) site not owned -> SITE_NOT_FOUND (AAA)")
  void handleRejectsForeignSite() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.isSiteOwnedBy(SITE_ID, OWNER_PROFILE_ID)).thenReturn(false);

    // Act
    var result = service.handle(new GetDashboardSummaryQuery(USERNAME, SITE_ID));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(alertRepository, alertPolicyRepository, externalServiceRequestService, externalDeviceService);
  }

  @Test
  @DisplayName("handle(GetDashboardSummaryQuery) -> classifies units, counts alerts and joins the other contexts (AAA)")
  void handleBuildsSummary() {
    // Arrange
    var now = Instant.now();
    var reporting = new MonitoredEquipment(EQUIPMENT_ID, "ONLINE", now.minusSeconds(10));
    var withOwnPolicy = new MonitoredEquipment(OTHER_EQUIPMENT_ID, "ONLINE", now.minusSeconds(200));
    var silent = new MonitoredEquipment(SILENT_ID, "OFFLINE", now.minus(Duration.ofHours(1)));
    var neverReported = new MonitoredEquipment(NEVER_REPORTED_ID, "OFFLINE", null);
    var equipment = List.of(reporting, withOwnPolicy, silent, neverReported);
    var ids = List.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID, SILENT_ID, NEVER_REPORTED_ID);
    var otherPolicy = new pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy(
        new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId(OTHER_EQUIPMENT_ID), 5, 1.5, 4);

    var critical = persistedAlert(AlertType.TEMPERATURE_EXCURSION, AlertSeverity.CRITICAL);
    var acknowledged = persistedAlert(AlertType.DEVICE_OFFLINE, AlertSeverity.WARNING);
    acknowledged.acknowledge(USER_ID);

    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, null)).thenReturn(equipment);
    when(externalAssetManagementService.countSitesOwnedBy(OWNER_PROFILE_ID)).thenReturn(3);
    when(externalAssetManagementService.fetchEquipmentStatusNames()).thenReturn(List.of("ONLINE", "OFFLINE", "MAINTENANCE"));
    when(alertPolicyRepository.findByEquipmentIdIn(ids)).thenReturn(List.of(otherPolicy));
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.empty());
    when(alertRepository.findByEquipmentIdInAndStatusIn(ids, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)))
        .thenReturn(List.<Alert>of(critical, acknowledged));
    when(externalServiceRequestService.countServiceRequests(OWNER_PROFILE_ID, null))
        .thenReturn(new ExternalServiceRequestServiceForMonitoring.ServiceRequestTotals(
            3L, Map.of("PENDING", 2L, "IN_PROGRESS", 1L)));
    when(externalDeviceService.countLinkedDevices(ids))
        .thenReturn(new ExternalDeviceServiceForMonitoring.LinkedDeviceTotals(4L, 1L));

    // Act
    var result = service.handle(new GetDashboardSummaryQuery(USERNAME, null));

    // Assert
    var summary = successValue(result);
    assertThat(summary.generatedAt()).isAfterOrEqualTo(now);
    assertThat(summary.siteCount()).isEqualTo(3);
    assertThat(summary.equipment().total()).isEqualTo(4);
    assertThat(summary.equipment().reporting()).isEqualTo(2);
    assertThat(summary.equipment().silent()).isEqualTo(1);
    assertThat(summary.equipment().neverReported()).isEqualTo(1);
    assertThat(summary.equipment().byStatus())
        .containsEntry("ONLINE", 2L).containsEntry("OFFLINE", 2L).containsEntry("MAINTENANCE", 0L);
    assertThat(summary.alerts().open()).isEqualTo(1);
    assertThat(summary.alerts().acknowledged()).isEqualTo(1);
    assertThat(summary.alerts().activeBySeverity())
        .containsEntry(AlertSeverity.CRITICAL, 1L)
        .containsEntry(AlertSeverity.WARNING, 1L)
        .containsEntry(AlertSeverity.INFO, 0L);
    assertThat(summary.serviceRequests().active()).isEqualTo(3);
    assertThat(summary.serviceRequests().byStatus()).containsEntry("PENDING", 2L).containsEntry("IN_PROGRESS", 1L);
    assertThat(summary.devices().paired()).isEqualTo(4);
    assertThat(summary.devices().revoked()).isEqualTo(1);
  }

  @Test
  @DisplayName("handle(GetDashboardSummaryQuery) with an owned site -> siteCount is 1 and sites are not counted (AAA)")
  void handleWithSiteReportsSingleSite() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.isSiteOwnedBy(SITE_ID, OWNER_PROFILE_ID)).thenReturn(true);
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, SITE_ID)).thenReturn(List.of());
    when(externalAssetManagementService.fetchEquipmentStatusNames()).thenReturn(List.of("ONLINE"));
    when(alertPolicyRepository.findByEquipmentIdIn(List.of())).thenReturn(List.of());
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.empty());
    when(alertRepository.findByEquipmentIdInAndStatusIn(List.of(), List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)))
        .thenReturn(List.of());
    when(externalServiceRequestService.countServiceRequests(OWNER_PROFILE_ID, SITE_ID))
        .thenReturn(new ExternalServiceRequestServiceForMonitoring.ServiceRequestTotals(0L, Map.of()));
    when(externalDeviceService.countLinkedDevices(List.of()))
        .thenReturn(new ExternalDeviceServiceForMonitoring.LinkedDeviceTotals(0L, 0L));

    // Act
    var summary = successValue(service.handle(new GetDashboardSummaryQuery(USERNAME, SITE_ID)));

    // Assert
    assertThat(summary.siteCount()).isEqualTo(1);
    assertThat(summary.equipment().total()).isZero();
    assertThat(summary.equipment().byStatus()).containsEntry("ONLINE", 0L);
    assertThat(summary.alerts().open()).isZero();
    assertThat(summary.alerts().activeBySeverity()).hasSize(AlertSeverity.values().length);
    verify(externalAssetManagementService, never()).countSitesOwnedBy(org.mockito.ArgumentMatchers.any());
  }
}
