package pe.edu.upc.ice.track.platform.device.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalMonitoringServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalMonitoringServiceForDevice.AlertPolicyValues;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalServiceRequestServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetEdgeConfigurationQuery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.EQUIPMENT_ID;

@ExtendWith(MockitoExtension.class)
class EdgeConfigurationQueryServiceImplTest {

  @Mock
  private ExternalAssetManagementServiceForDevice externalAssetManagementService;
  @Mock
  private ExternalMonitoringServiceForDevice externalMonitoringService;
  @Mock
  private ExternalServiceRequestServiceForDevice externalServiceRequestService;

  @InjectMocks
  private EdgeConfigurationQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetEdgeConfigurationQuery) -> assembles version, threshold, alert policy and grants (AAA)")
  void handleSuccess() {
    // Arrange
    var expiresAt = Instant.parse("2026-02-01T00:00:00Z");
    var grant = new EdgeConfiguration.Grant("abc123", 5L, 9L, expiresAt);
    when(externalAssetManagementService.fetchConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(4));
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));
    when(externalMonitoringService.fetchApplicableAlertPolicy(EQUIPMENT_ID)).thenReturn(new AlertPolicyValues(15, 0.5, 3));
    when(externalServiceRequestService.fetchActiveEdgeGrants(EQUIPMENT_ID)).thenReturn(List.of(grant));

    // Act
    var result = service.handle(new GetEdgeConfigurationQuery(EQUIPMENT_ID));

    // Assert
    assertThat(result).isPresent();
    var configuration = result.get();
    assertThat(configuration.configVersion()).isEqualTo(4);
    assertThat(configuration.minCelsius()).isEqualTo(2.0);
    assertThat(configuration.maxCelsius()).isEqualTo(8.0);
    assertThat(configuration.sustainedExcursionMinutes()).isEqualTo(15);
    assertThat(configuration.hysteresisMarginCelsius()).isEqualTo(0.5);
    assertThat(configuration.missedSyncWindowsForOffline()).isEqualTo(3);
    assertThat(configuration.grants()).containsExactly(grant);
  }

  @Test
  @DisplayName("handle(GetEdgeConfigurationQuery) -> succeeds with no grants when none is active (AAA)")
  void handleWithoutGrants() {
    // Arrange
    when(externalAssetManagementService.fetchConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(1));
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));
    when(externalMonitoringService.fetchApplicableAlertPolicy(EQUIPMENT_ID)).thenReturn(new AlertPolicyValues(10, 0.3, 2));
    when(externalServiceRequestService.fetchActiveEdgeGrants(EQUIPMENT_ID)).thenReturn(List.of());

    // Act
    var result = service.handle(new GetEdgeConfigurationQuery(EQUIPMENT_ID));

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().grants()).isEmpty();
  }

  @Test
  @DisplayName("handle(GetEdgeConfigurationQuery) -> empty when the equipment has no config version, asking nothing else (AAA)")
  void handleNoConfigVersion() {
    // Arrange
    when(externalAssetManagementService.fetchConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetEdgeConfigurationQuery(EQUIPMENT_ID));

    // Assert
    assertThat(result).isEmpty();
    verify(externalAssetManagementService, times(1)).fetchConfigVersion(EQUIPMENT_ID);
    verifyNoInteractions(externalMonitoringService, externalServiceRequestService);
  }

  @Test
  @DisplayName("handle(GetEdgeConfigurationQuery) -> empty when the equipment has no threshold, skipping monitoring and grants (AAA)")
  void handleNoThreshold() {
    // Arrange
    when(externalAssetManagementService.fetchConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(2));
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetEdgeConfigurationQuery(EQUIPMENT_ID));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(externalMonitoringService, externalServiceRequestService);
  }

  @Test
  @DisplayName("GetEdgeConfigurationQuery(Long) -> rejects a null equipment id (AAA)")
  void queryRejectsNullEquipmentId() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new GetEdgeConfigurationQuery(null));
  }
}
