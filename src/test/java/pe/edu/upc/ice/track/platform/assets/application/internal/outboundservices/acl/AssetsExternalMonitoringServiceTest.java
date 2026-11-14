package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.EQUIPMENT_ID;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.OTHER_EQUIPMENT_ID;

@ExtendWith(MockitoExtension.class)
class AssetsExternalMonitoringServiceTest {

  @Mock
  private MonitoringContextFacade monitoringContextFacade;

  @InjectMocks
  private AssetsExternalMonitoringService service;

  @Test
  @DisplayName("fetchOfflineSilence(Collection) -> converts the seconds answered by monitoring into durations (AAA)")
  void fetchOfflineSilenceSuccess() {
    // Arrange
    var ids = Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    when(monitoringContextFacade.fetchOfflineSilenceSeconds(ids))
        .thenReturn(Map.of(EQUIPMENT_ID, 180L, OTHER_EQUIPMENT_ID, 600L));

    // Act
    var result = service.fetchOfflineSilence(ids);

    // Assert
    assertThat(result)
        .containsEntry(EQUIPMENT_ID, Duration.ofSeconds(180))
        .containsEntry(OTHER_EQUIPMENT_ID, Duration.ofMinutes(10))
        .hasSize(2);
    verify(monitoringContextFacade, times(1)).fetchOfflineSilenceSeconds(ids);
  }

  @Test
  @DisplayName("fetchOfflineSilence(Collection) -> omits units monitoring did not answer for (AAA)")
  void fetchOfflineSilencePartialAnswer() {
    // Arrange
    var ids = List.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    when(monitoringContextFacade.fetchOfflineSilenceSeconds(ids)).thenReturn(Map.of(EQUIPMENT_ID, 60L));

    // Act
    var result = service.fetchOfflineSilence(ids);

    // Assert
    assertThat(result).containsOnlyKeys(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("fetchOfflineSilence(Collection) -> returns an empty map without calling monitoring for null ids (AAA)")
  void fetchOfflineSilenceNullIds() {
    // Act
    var result = service.fetchOfflineSilence(null);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(monitoringContextFacade);
  }

  @Test
  @DisplayName("fetchOfflineSilence(Collection) -> returns an empty map without calling monitoring for empty ids (AAA)")
  void fetchOfflineSilenceEmptyIds() {
    // Act
    var result = service.fetchOfflineSilence(List.of());

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(monitoringContextFacade);
  }

  @Test
  @DisplayName("fetchOfflineSilence(Collection) -> returns an unmodifiable map (AAA)")
  void fetchOfflineSilenceUnmodifiable() {
    // Arrange
    var ids = List.of(EQUIPMENT_ID);
    when(monitoringContextFacade.fetchOfflineSilenceSeconds(ids)).thenReturn(Map.of(EQUIPMENT_ID, 60L));

    // Act
    var result = service.fetchOfflineSilence(ids);

    // Assert
    assertThrows(UnsupportedOperationException.class, () -> result.put(99L, Duration.ZERO));
  }
}
