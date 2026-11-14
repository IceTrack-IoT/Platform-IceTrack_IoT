package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.AlertPolicySettings;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.EQUIPMENT_ID;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.OTHER_EQUIPMENT_ID;

@ExtendWith(MockitoExtension.class)
class ExternalMonitoringServiceForDeviceTest {

  @Mock
  private MonitoringContextFacade monitoringContextFacade;

  @InjectMocks
  private ExternalMonitoringServiceForDevice service;

  @Test
  @DisplayName("fetchApplicableAlertPolicy(Long) -> copies the three values of the monitoring settings (AAA)")
  void fetchApplicableAlertPolicy() {
    // Arrange
    when(monitoringContextFacade.fetchApplicableAlertPolicy(EQUIPMENT_ID)).thenReturn(new AlertPolicySettings(15, 0.5, 3));

    // Act
    var result = service.fetchApplicableAlertPolicy(EQUIPMENT_ID);

    // Assert
    assertThat(result.sustainedExcursionMinutes()).isEqualTo(15);
    assertThat(result.hysteresisMarginCelsius()).isEqualTo(0.5);
    assertThat(result.missedSyncWindowsForOffline()).isEqualTo(3);
  }

  @Test
  @DisplayName("fetchOfflineWindows(Collection) -> converts the seconds answered by monitoring into durations (AAA)")
  void fetchOfflineWindowsConvertsSeconds() {
    // Arrange
    var ids = Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    when(monitoringContextFacade.fetchOfflineSilenceSeconds(ids))
        .thenReturn(Map.of(EQUIPMENT_ID, 180L, OTHER_EQUIPMENT_ID, 600L));

    // Act
    var result = service.fetchOfflineWindows(ids);

    // Assert
    assertThat(result)
        .containsEntry(EQUIPMENT_ID, Duration.ofSeconds(180))
        .containsEntry(OTHER_EQUIPMENT_ID, Duration.ofMinutes(10))
        .hasSize(2);
    verify(monitoringContextFacade, times(1)).fetchOfflineSilenceSeconds(ids);
  }

  @Test
  @DisplayName("fetchOfflineWindows(Collection) -> empty map without calling monitoring for null ids (AAA)")
  void fetchOfflineWindowsNullIds() {
    // Act & Assert
    assertThat(service.fetchOfflineWindows(null)).isEmpty();
    verifyNoInteractions(monitoringContextFacade);
  }

  @Test
  @DisplayName("fetchOfflineWindows(Collection) -> empty map without calling monitoring for empty ids (AAA)")
  void fetchOfflineWindowsEmptyIds() {
    // Act & Assert
    assertThat(service.fetchOfflineWindows(List.of())).isEmpty();
    verifyNoInteractions(monitoringContextFacade);
  }
}
