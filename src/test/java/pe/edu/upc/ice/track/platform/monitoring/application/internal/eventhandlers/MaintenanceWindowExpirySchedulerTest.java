package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices.MaintenanceWindowEventProcessor;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowExpirySchedulerTest {

  @Mock
  private MaintenanceWindowEventProcessor maintenanceWindowEventProcessor;

  @Test
  @DisplayName("new MaintenanceWindowExpiryScheduler(negative grace) -> IllegalStateException (AAA)")
  void constructorRejectsNegativeGrace() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new MaintenanceWindowExpiryScheduler(maintenanceWindowEventProcessor, -1))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("closeOverdueWindows() -> sweeps with the configured grace measured from now (AAA)")
  void sweepsWithConfiguredGrace() {
    // Arrange
    var scheduler = new MaintenanceWindowExpiryScheduler(maintenanceWindowEventProcessor, 30);
    when(maintenanceWindowEventProcessor.closeExpired(any(Instant.class), eq(Duration.ofMinutes(30)))).thenReturn(2);
    var before = Instant.now();

    // Act
    scheduler.closeOverdueWindows();

    // Assert
    var now = ArgumentCaptor.forClass(Instant.class);
    verify(maintenanceWindowEventProcessor, times(1)).closeExpired(now.capture(), eq(Duration.ofMinutes(30)));
    assertThat(now.getValue()).isBetween(before, Instant.now());
  }

  @Test
  @DisplayName("closeOverdueWindows() with zero grace and nothing overdue -> completes quietly (AAA)")
  void zeroGraceNothingOverdue() {
    // Arrange
    var scheduler = new MaintenanceWindowExpiryScheduler(maintenanceWindowEventProcessor, 0);
    when(maintenanceWindowEventProcessor.closeExpired(any(Instant.class), eq(Duration.ZERO))).thenReturn(0);

    // Act & Assert
    assertThatCode(scheduler::closeOverdueWindows).doesNotThrowAnyException();
    verify(maintenanceWindowEventProcessor, times(1)).closeExpired(any(Instant.class), eq(Duration.ZERO));
  }

  @Test
  @DisplayName("closeOverdueWindows() when the sweep fails -> swallows the error (AAA)")
  void swallowsSweepFailure() {
    // Arrange
    var scheduler = new MaintenanceWindowExpiryScheduler(maintenanceWindowEventProcessor, 30);
    when(maintenanceWindowEventProcessor.closeExpired(any(Instant.class), any(Duration.class)))
        .thenThrow(new IllegalStateException("database unavailable"));

    // Act & Assert
    assertThatCode(scheduler::closeOverdueWindows).doesNotThrowAnyException();
  }
}
