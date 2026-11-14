package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.EquipmentConnectivityCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DetectOfflineEquipmentCommand;
import pe.edu.upc.ice.track.platform.shared.application.scheduling.ScheduledTaskLock;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfflineDetectionSchedulerTest {

  private static final long LEASE_MILLIS = 50_000L;

  @Mock
  private EquipmentConnectivityCommandService equipmentConnectivityCommandService;
  @Mock
  private ScheduledTaskLock scheduledTaskLock;

  private OfflineDetectionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new OfflineDetectionScheduler(equipmentConnectivityCommandService, scheduledTaskLock, true, LEASE_MILLIS);
  }

  @Test
  @DisplayName("new OfflineDetectionScheduler(lease < 1) -> IllegalStateException (AAA)")
  void constructorRejectsNonPositiveLease() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new OfflineDetectionScheduler(equipmentConnectivityCommandService, scheduledTaskLock, true, 0))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("detectOfflineEquipment() disabled -> does nothing (AAA)")
  void disabledSchedulerDoesNothing() {
    // Arrange
    var disabled = new OfflineDetectionScheduler(equipmentConnectivityCommandService, scheduledTaskLock, false, LEASE_MILLIS);

    // Act
    disabled.detectOfflineEquipment();

    // Assert
    verifyNoInteractions(scheduledTaskLock, equipmentConnectivityCommandService);
    assertThat(disabled.lastCompletedRunAt()).isNull();
  }

  @Test
  @DisplayName("detectOfflineEquipment() without the lock -> skips the round (AAA)")
  void skipsWhenAnotherInstanceHoldsTheLock() {
    // Arrange
    when(scheduledTaskLock.tryAcquire(OfflineDetectionScheduler.TASK_NAME, Duration.ofMillis(LEASE_MILLIS)))
        .thenReturn(false);

    // Act
    scheduler.detectOfflineEquipment();

    // Assert
    verifyNoInteractions(equipmentConnectivityCommandService);
    assertThat(scheduler.lastCompletedRunAt()).isNull();
  }

  @Test
  @DisplayName("detectOfflineEquipment() with the lock -> runs detection stamped now and records completion (AAA)")
  void runsDetectionWhenLockAcquired() {
    // Arrange
    when(scheduledTaskLock.tryAcquire(OfflineDetectionScheduler.TASK_NAME, Duration.ofMillis(LEASE_MILLIS)))
        .thenReturn(true);
    when(equipmentConnectivityCommandService.handle(any(DetectOfflineEquipmentCommand.class))).thenReturn(2);
    var before = Instant.now();

    // Act
    scheduler.detectOfflineEquipment();

    // Assert
    var captor = ArgumentCaptor.forClass(DetectOfflineEquipmentCommand.class);
    verify(equipmentConnectivityCommandService, times(1)).handle(captor.capture());
    assertThat(captor.getValue().detectedAt()).isBetween(before, Instant.now());
    assertThat(scheduler.lastCompletedRunAt()).isNotNull().isAfterOrEqualTo(before);
  }

  @Test
  @DisplayName("detectOfflineEquipment() when nothing went silent -> still records completion (AAA)")
  void recordsCompletionWhenNothingRaised() {
    // Arrange
    when(scheduledTaskLock.tryAcquire(OfflineDetectionScheduler.TASK_NAME, Duration.ofMillis(LEASE_MILLIS)))
        .thenReturn(true);
    when(equipmentConnectivityCommandService.handle(any(DetectOfflineEquipmentCommand.class))).thenReturn(0);

    // Act
    scheduler.detectOfflineEquipment();

    // Assert
    assertThat(scheduler.lastCompletedRunAt()).isNotNull();
  }

  @Test
  @DisplayName("detectOfflineEquipment() when detection fails -> swallows the error, no completion recorded (AAA)")
  void swallowsDetectionFailure() {
    // Arrange
    when(scheduledTaskLock.tryAcquire(OfflineDetectionScheduler.TASK_NAME, Duration.ofMillis(LEASE_MILLIS)))
        .thenReturn(true);
    when(equipmentConnectivityCommandService.handle(any(DetectOfflineEquipmentCommand.class)))
        .thenThrow(new IllegalStateException("database unavailable"));

    // Act
    scheduler.detectOfflineEquipment();

    // Assert
    assertThat(scheduler.lastCompletedRunAt()).isNull();
  }

  @Test
  @DisplayName("detectOfflineEquipment() when the lock fails -> swallows the error and does not detect (AAA)")
  void swallowsLockFailure() {
    // Arrange
    when(scheduledTaskLock.tryAcquire(OfflineDetectionScheduler.TASK_NAME, Duration.ofMillis(LEASE_MILLIS)))
        .thenThrow(new IllegalStateException("lock table unavailable"));

    // Act
    scheduler.detectOfflineEquipment();

    // Assert
    verify(equipmentConnectivityCommandService, never()).handle(any());
  }
}
