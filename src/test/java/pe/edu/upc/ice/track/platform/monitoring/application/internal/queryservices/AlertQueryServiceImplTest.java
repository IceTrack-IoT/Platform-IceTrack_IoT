package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.MonitoredEquipment;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertsQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertPage;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class AlertQueryServiceImplTest {

  @Mock
  private AlertRepository alertRepository;
  @Mock
  private EquipmentAccessPolicy equipmentAccessPolicy;
  @Mock
  private ExternalProfilesServiceForMonitoring externalProfilesService;
  @Mock
  private ExternalAssetManagementService externalAssetManagementService;

  @InjectMocks
  private AlertQueryServiceImpl service;

  private static GetAlertsQuery alertsQuery(Long equipmentId, Long siteId) {
    return new GetAlertsQuery(USERNAME, Set.of(AlertStatus.OPEN), equipmentId, siteId,
        Set.of(AlertType.TEMPERATURE_EXCURSION), Set.of(AlertSeverity.CRITICAL), T0, T0.plusSeconds(3600), 2, 20);
  }

  private static MonitoredEquipment unit(Long equipmentId) {
    return new MonitoredEquipment(equipmentId, "ONLINE", T0);
  }

  @SuppressWarnings("unchecked")
  private Collection<Long> scopeUsedForPage() {
    var captor = ArgumentCaptor.forClass(Collection.class);
    verify(alertRepository, times(1)).findPageByEquipmentIdIn(
        captor.capture(), any(), any(), any(), any(), any(), anyInt(), anyInt());
    return (Collection<Long>) captor.getValue();
  }


  @Test
  @DisplayName("handle(GetAlertByIdQuery) readable alert -> returns it (AAA)")
  void getByIdReturnsReadableAlert() {
    // Arrange
    var alert = persistedTemperatureAlert();
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
    when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(true);

    // Act
    var result = service.handle(new GetAlertByIdQuery(ALERT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).containsSame(alert);
  }

  @Test
  @DisplayName("handle(GetAlertByIdQuery) alert of an unreadable equipment -> empty (AAA)")
  void getByIdHidesUnreadableAlert() {
    // Arrange
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(persistedTemperatureAlert()));
    when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(false);

    // Act
    var result = service.handle(new GetAlertByIdQuery(ALERT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("handle(GetAlertByIdQuery) unknown alert -> empty without checking access (AAA)")
  void getByIdUnknownAlert() {
    // Arrange
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetAlertByIdQuery(ALERT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(equipmentAccessPolicy);
  }

  @Test
  @DisplayName("handle(GetOpenAlertsByEquipmentQuery) readable -> OPEN and ACKNOWLEDGED alerts (AAA)")
  void getOpenAlertsReturnsActiveAlerts() {
    // Arrange
    var alerts = List.of(persistedTemperatureAlert());
    when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(true);
    when(alertRepository.findByEquipmentIdAndStatusIn(
        EQUIPMENT_ID, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED))).thenReturn(alerts);

    // Act
    var result = service.handle(new GetOpenAlertsByEquipmentQuery(EQUIPMENT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).isEqualTo(alerts);
  }

  @Test
  @DisplayName("handle(GetOpenAlertsByEquipmentQuery) unreadable -> empty list without querying alerts (AAA)")
  void getOpenAlertsDeniedReturnsEmpty() {
    // Arrange
    when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(false);

    // Act
    var result = service.handle(new GetOpenAlertsByEquipmentQuery(EQUIPMENT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(alertRepository);
  }

  @Test
  @DisplayName("handle(GetAlertsQuery) site not owned -> SITE_NOT_FOUND (AAA)")
  void getAlertsRejectsForeignSite() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.isSiteOwnedBy(SITE_ID, OWNER_PROFILE_ID)).thenReturn(false);

    // Act
    var result = service.handle(alertsQuery(null, SITE_ID));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(alertRepository);
  }

  @Test
  @DisplayName("handle(GetAlertsQuery) equipment not owned -> EQUIPMENT_NOT_FOUND (AAA)")
  void getAlertsRejectsForeignEquipment() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(false);

    // Act
    var result = service.handle(alertsQuery(EQUIPMENT_ID, null));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("EQUIPMENT_NOT_FOUND");
    verifyNoInteractions(alertRepository);
  }

  @Test
  @DisplayName("handle(GetAlertsQuery) without filters on location -> pages over every owned equipment (AAA)")
  void getAlertsScopesToAllOwnedEquipment() {
    // Arrange
    var page = new AlertPage(List.of(persistedTemperatureAlert()), 2, 20, 41);
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, null))
        .thenReturn(List.of(unit(EQUIPMENT_ID), unit(OTHER_EQUIPMENT_ID)));
    when(alertRepository.findPageByEquipmentIdIn(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(page);

    // Act
    var result = service.handle(alertsQuery(null, null));

    // Assert
    assertThat(successValue(result)).isSameAs(page);
    assertThat(scopeUsedForPage()).containsExactlyInAnyOrder(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    verify(alertRepository, times(1)).findPageByEquipmentIdIn(
        any(), org.mockito.ArgumentMatchers.eq(Set.of(AlertStatus.OPEN)),
        org.mockito.ArgumentMatchers.eq(Set.of(AlertType.TEMPERATURE_EXCURSION)),
        org.mockito.ArgumentMatchers.eq(Set.of(AlertSeverity.CRITICAL)),
        org.mockito.ArgumentMatchers.eq(T0), org.mockito.ArgumentMatchers.eq(T0.plusSeconds(3600)), anyInt(), anyInt());
  }

  @Test
  @DisplayName("handle(GetAlertsQuery) with an owned equipment -> scope narrows to that equipment (AAA)")
  void getAlertsNarrowsToEquipment() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(true);
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, null))
        .thenReturn(List.of(unit(EQUIPMENT_ID), unit(OTHER_EQUIPMENT_ID)));
    when(alertRepository.findPageByEquipmentIdIn(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new AlertPage(List.of(), 2, 20, 0));

    // Act
    var result = service.handle(alertsQuery(EQUIPMENT_ID, null));

    // Assert
    assertThat(result).isNotNull();
    assertThat(scopeUsedForPage()).containsExactly(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("handle(GetAlertsQuery) equipment outside the requested site -> empty scope (AAA)")
  void getAlertsEmptyScopeWhenEquipmentOutsideSite() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.isSiteOwnedBy(SITE_ID, OWNER_PROFILE_ID)).thenReturn(true);
    when(externalAssetManagementService.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(true);
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, SITE_ID))
        .thenReturn(List.of(unit(OTHER_EQUIPMENT_ID)));
    when(alertRepository.findPageByEquipmentIdIn(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new AlertPage(List.of(), 2, 20, 0));

    // Act
    service.handle(alertsQuery(EQUIPMENT_ID, SITE_ID));

    // Assert
    assertThat(scopeUsedForPage()).isEmpty();
  }

  @Test
  @DisplayName("handle(GetAlertsQuery) caller without owner profile -> pages over an empty scope (AAA)")
  void getAlertsWithoutOwnerProfile() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
    when(externalAssetManagementService.fetchEquipmentOwnedBy(null, null)).thenReturn(List.of());
    when(alertRepository.findPageByEquipmentIdIn(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new AlertPage(List.of(), 2, 20, 0));

    // Act
    var result = service.handle(alertsQuery(null, null));

    // Assert
    assertThat(successValue(result).content()).isEmpty();
    assertThat(scopeUsedForPage()).isEmpty();
    verify(externalAssetManagementService, never()).isSiteOwnedBy(any(), any());
  }
}
