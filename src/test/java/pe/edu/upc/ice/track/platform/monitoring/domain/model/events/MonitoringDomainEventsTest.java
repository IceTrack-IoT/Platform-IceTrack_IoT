package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class MonitoringDomainEventsTest {

  @Test
  @DisplayName("MaintenanceWindowOpenedEvent.from(MaintenanceWindow) -> copies the window's data (AAA)")
  void openedEventFromWindow() {
    // Arrange
    var window = persistedAppWindow();

    // Act
    var event = MaintenanceWindowOpenedEvent.from(window);

    // Assert
    assertThat(event).isEqualTo(new MaintenanceWindowOpenedEvent(WINDOW_ID, START_EVENT_UID, EQUIPMENT_ID,
        MaintenanceOrigin.APP, T0, window.getExpiresAt(), TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID));
  }

  @Test
  @DisplayName("MaintenanceWindowClosedEvent.from(MaintenanceWindow) -> copies the closure data (AAA)")
  void closedEventFromWindow() {
    // Arrange
    var window = persistedAppWindow();
    window.close(T0.plusSeconds(60), ClosureReason.INTERVENTION_COMPLETED);

    // Act
    var event = MaintenanceWindowClosedEvent.from(window);

    // Assert
    assertThat(event).isEqualTo(new MaintenanceWindowClosedEvent(WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.APP, T0,
        T0.plusSeconds(60), ClosureReason.INTERVENTION_COMPLETED, TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID));
  }

  @Test
  @DisplayName("MaintenanceWindowPromotedEvent.from(MaintenanceWindow, Instant) -> uses the supplied promotion instant (AAA)")
  void promotedEventFromWindow() {
    // Arrange
    var window = persistedAppWindow();
    var promotedAt = T0.plusSeconds(30);

    // Act
    var event = MaintenanceWindowPromotedEvent.from(window, promotedAt);

    // Assert
    assertThat(event.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(event.startedAt()).isEqualTo(T0);
    assertThat(event.promotedAt()).isEqualTo(promotedAt);
    assertThat(event.expiresAt()).isEqualTo(window.getExpiresAt());
    assertThat(event.technicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
  }
}
