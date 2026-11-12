package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowClosedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowOpenedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowPromotedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class MaintenanceWindowTest {

  @Test
  @DisplayName("open(...) -> creates an active window that expires after the origin's lifetime (AAA)")
  void openCreatesActiveWindow() {
    // Act
    var window = MaintenanceWindow.open(START_EVENT_UID, EQUIPMENT_ID, MaintenanceOrigin.APP, T0,
        TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID);

    // Assert
    assertThat(window.getId()).isNull();
    assertThat(window.getStartEventUid()).isEqualTo(START_EVENT_UID);
    assertThat(window.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(window.getOrigin()).isEqualTo(MaintenanceOrigin.APP);
    assertThat(window.getStartedAt()).isEqualTo(T0);
    assertThat(window.getExpiresAt()).isEqualTo(T0.plus(Duration.ofHours(2)));
    assertThat(window.isActive()).isTrue();
    assertThat(window.getEndedAt()).isNull();
    assertThat(window.getClosureReason()).isNull();
    assertThat(window.getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
    assertThat(window.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(window.isUnlinked()).isFalse();
  }

  @Test
  @DisplayName("open(...) -> a BUTTON window lasts 30 minutes and is unlinked (AAA)")
  void openButtonWindow() {
    var window = buttonWindow();

    assertThat(window.getExpiresAt()).isEqualTo(T0.plus(Duration.ofMinutes(30)));
    assertThat(window.isUnlinked()).isTrue();
  }

  @Test
  @DisplayName("open(...) -> requires the origin (AAA)")
  void openRequiresOrigin() {
    assertThrows(NullPointerException.class,
        () -> MaintenanceWindow.open(START_EVENT_UID, EQUIPMENT_ID, null, T0, null, null));
  }

  @Test
  @DisplayName("MaintenanceWindow(...) -> requires event uid, equipment, origin, start and expiry (AAA)")
  void constructorRequiresMandatoryFields() {
    var expiry = T0.plusSeconds(60);
    assertThrows(NullPointerException.class, () -> new MaintenanceWindow(null, null, EQUIPMENT_ID,
        MaintenanceOrigin.APP, T0, expiry, null, null, null, null));
    assertThrows(NullPointerException.class, () -> new MaintenanceWindow(null, START_EVENT_UID, null,
        MaintenanceOrigin.APP, T0, expiry, null, null, null, null));
    assertThrows(NullPointerException.class, () -> new MaintenanceWindow(null, START_EVENT_UID, EQUIPMENT_ID,
        null, T0, expiry, null, null, null, null));
    assertThrows(NullPointerException.class, () -> new MaintenanceWindow(null, START_EVENT_UID, EQUIPMENT_ID,
        MaintenanceOrigin.APP, null, expiry, null, null, null, null));
    assertThrows(NullPointerException.class, () -> new MaintenanceWindow(null, START_EVENT_UID, EQUIPMENT_ID,
        MaintenanceOrigin.APP, T0, null, null, null, null, null));
  }

  @Test
  @DisplayName("MaintenanceWindow(...) -> rejects an expiry that is not after the start (AAA)")
  void constructorRejectsNonPositiveSpan() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new MaintenanceWindow(null, START_EVENT_UID,
        EQUIPMENT_ID, MaintenanceOrigin.APP, T0, T0, null, null, null, null));
    assertThat(ex).hasMessage("A maintenance window must expire after it starts");
  }

  @Test
  @DisplayName("MaintenanceWindow(...) -> an end and a closure reason come together or not at all (AAA)")
  void constructorRequiresEndAndReasonTogether() {
    var expiry = T0.plusSeconds(60);
    assertThrows(IllegalArgumentException.class, () -> new MaintenanceWindow(null, START_EVENT_UID, EQUIPMENT_ID,
        MaintenanceOrigin.APP, T0, expiry, T0.plusSeconds(10), null, null, null));
    assertThrows(IllegalArgumentException.class, () -> new MaintenanceWindow(null, START_EVENT_UID, EQUIPMENT_ID,
        MaintenanceOrigin.APP, T0, expiry, null, ClosureReason.EXPIRED, null, null));
  }

  @Test
  @DisplayName("MaintenanceWindow(...) -> rejects an end before the start (AAA)")
  void constructorRejectsEndBeforeStart() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new MaintenanceWindow(null, START_EVENT_UID,
        EQUIPMENT_ID, MaintenanceOrigin.APP, T0, T0.plusSeconds(60), T0.minusSeconds(1),
        ClosureReason.EXPIRED, null, null));
    assertThat(ex).hasMessage("A maintenance window cannot end before it starts");
  }

  @Test
  @DisplayName("closedWindow fixture -> is inactive with its closure reason (AAA)")
  void closedFixtureIsInactive() {
    var window = closedWindow();

    assertThat(window.isActive()).isFalse();
    assertThat(window.getClosureReason()).isEqualTo(ClosureReason.MANUAL_BUTTON);
  }


  @Test
  @DisplayName("onOpened() -> registers MaintenanceWindowOpenedEvent once persisted (AAA)")
  void onOpenedRegistersEvent() {
    // Arrange
    var window = persistedAppWindow();

    // Act
    window.onOpened();

    // Assert
    assertThat(window.domainEvents()).containsExactly(MaintenanceWindowOpenedEvent.from(window));
    var event = (MaintenanceWindowOpenedEvent) window.domainEvents().iterator().next();
    assertThat(event.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(event.expiresAt()).isEqualTo(window.getExpiresAt());
  }

  @Test
  @DisplayName("onOpened() -> rejects a window that has not been persisted (AAA)")
  void onOpenedRequiresPersistence() {
    var window = buttonWindow();

    var ex = assertThrows(IllegalStateException.class, window::onOpened);

    assertThat(ex).hasMessage("A maintenance window must be persisted before it is opened");
  }

  @Test
  @DisplayName("promoteToApp(...) -> upgrades an active BUTTON window to APP and links the order (AAA)")
  void promoteUpgradesButtonWindow() {
    // Arrange
    var window = persistedButtonWindow();
    var newExpiry = T0.plus(Duration.ofHours(2));

    // Act
    var promoted = window.promoteToApp(TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID, newExpiry);

    // Assert
    assertThat(promoted).isTrue();
    assertThat(window.getOrigin()).isEqualTo(MaintenanceOrigin.APP);
    assertThat(window.getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
    assertThat(window.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(window.getExpiresAt()).isEqualTo(newExpiry);
    var event = (MaintenanceWindowPromotedEvent) window.domainEvents().iterator().next();
    assertThat(event.promotedAt()).isEqualTo(newExpiry.minus(MaintenanceOrigin.APP.lifetime()));
    assertThat(event.expiresAt()).isEqualTo(newExpiry);
  }

  @Test
  @DisplayName("promoteToApp(...) -> keeps existing links when the new ones are null (AAA)")
  void promoteKeepsLinksWhenNull() {
    // Arrange
    var window = persistedButtonWindow();
    window.linkTo(TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID);

    // Act
    window.promoteToApp(null, null, T0.plus(Duration.ofHours(2)));

    // Assert
    assertThat(window.getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
    assertThat(window.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("promoteToApp(...) -> an unsaved window is promoted silently, without an event (AAA)")
  void promoteUnsavedWindowIsSilent() {
    var window = buttonWindow();

    var promoted = window.promoteToApp(null, null, T0.plus(Duration.ofHours(2)));

    assertThat(promoted).isTrue();
    assertThat(window.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("promoteToApp(...) -> does nothing for a window that is not BUTTON-origin (AAA)")
  void promoteIgnoresAppWindow() {
    var window = persistedAppWindow();
    var expiry = window.getExpiresAt();

    var promoted = window.promoteToApp(null, null, T0.plus(Duration.ofHours(5)));

    assertThat(promoted).isFalse();
    assertThat(window.getExpiresAt()).isEqualTo(expiry);
    assertThat(window.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("promoteToApp(...) -> does nothing for a window that already ended (AAA)")
  void promoteIgnoresClosedWindow() {
    var window = persistedButtonWindow();
    window.close(T0.plusSeconds(60), ClosureReason.MANUAL_BUTTON);

    assertThat(window.promoteToApp(null, null, T0.plus(Duration.ofHours(2)))).isFalse();
    assertThat(window.getOrigin()).isEqualTo(MaintenanceOrigin.BUTTON);
  }

  @Test
  @DisplayName("promoteToApp(...) -> requires an expiry after the start (AAA)")
  void promoteRequiresValidExpiry() {
    var window = persistedButtonWindow();

    assertThrows(NullPointerException.class, () -> window.promoteToApp(null, null, null));
    assertThrows(IllegalArgumentException.class, () -> window.promoteToApp(null, null, T0));
    assertThat(window.getOrigin()).isEqualTo(MaintenanceOrigin.BUTTON);
  }

  @Test
  @DisplayName("linkTo(Long, Long) -> links an unlinked active window (AAA)")
  void linkToLinksUnlinkedWindow() {
    var window = buttonWindow();

    var linked = window.linkTo(TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID);

    assertThat(linked).isTrue();
    assertThat(window.isUnlinked()).isFalse();
    assertThat(window.getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
  }

  @Test
  @DisplayName("linkTo(Long, Long) -> does not overwrite an existing link (AAA)")
  void linkToKeepsExistingLink() {
    var window = appWindow();

    var linked = window.linkTo(999L, 998L);

    assertThat(linked).isFalse();
    assertThat(window.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("linkTo(Long, Long) -> does not link a window that already ended (AAA)")
  void linkToIgnoresClosedWindow() {
    var window = persistedButtonWindow();
    window.close(T0.plusSeconds(60), ClosureReason.EXPIRED);

    assertThat(window.linkTo(TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID)).isFalse();
    assertThat(window.isUnlinked()).isTrue();
  }

  @Test
  @DisplayName("linkTo(Long, Long) -> requires both identifiers (AAA)")
  void linkToRequiresIds() {
    var window = buttonWindow();

    assertThrows(NullPointerException.class, () -> window.linkTo(null, SERVICE_REQUEST_ID));
    assertThrows(NullPointerException.class, () -> window.linkTo(TECHNICIAN_PROFILE_ID, null));
  }

  @Test
  @DisplayName("close(Instant, ClosureReason) -> ends the window and announces the closure (AAA)")
  void closeEndsWindow() {
    // Arrange
    var window = persistedButtonWindow();
    var endedAt = T0.plusSeconds(600);

    // Act
    var closed = window.close(endedAt, ClosureReason.INTERVENTION_COMPLETED);

    // Assert
    assertThat(closed).isTrue();
    assertThat(window.isActive()).isFalse();
    assertThat(window.getEndedAt()).isEqualTo(endedAt);
    assertThat(window.getClosureReason()).isEqualTo(ClosureReason.INTERVENTION_COMPLETED);
    assertThat(window.domainEvents()).containsExactly(MaintenanceWindowClosedEvent.from(window));
  }

  @Test
  @DisplayName("close(Instant, ClosureReason) -> an end before the start is clamped to the start (AAA)")
  void closeClampsToStart() {
    var window = persistedButtonWindow();

    window.close(T0.minusSeconds(60), ClosureReason.EXPIRED);

    assertThat(window.getEndedAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("close(Instant, ClosureReason) -> is idempotent: closing twice changes nothing (AAA)")
  void closeIsIdempotent() {
    // Arrange
    var window = persistedButtonWindow();
    window.close(T0.plusSeconds(60), ClosureReason.MANUAL_BUTTON);
    window.clearDomainEvents();

    // Act
    var closedAgain = window.close(T0.plusSeconds(120), ClosureReason.EXPIRED);

    // Assert
    assertThat(closedAgain).isFalse();
    assertThat(window.getEndedAt()).isEqualTo(T0.plusSeconds(60));
    assertThat(window.getClosureReason()).isEqualTo(ClosureReason.MANUAL_BUTTON);
    assertThat(window.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("close(Instant, ClosureReason) -> requires the end and the reason (AAA)")
  void closeRequiresArguments() {
    var window = persistedButtonWindow();

    assertThrows(NullPointerException.class, () -> window.close(null, ClosureReason.EXPIRED));
    assertThrows(NullPointerException.class, () -> window.close(T0, null));
  }

  @Test
  @DisplayName("close(Instant, ClosureReason) -> rejects a window that has not been persisted (AAA)")
  void closeRequiresPersistence() {
    var window = buttonWindow();

    var ex = assertThrows(IllegalStateException.class, () -> window.close(T0.plusSeconds(1), ClosureReason.EXPIRED));

    assertThat(ex).hasMessage("A maintenance window must be persisted before it is closed");
  }

  @Test
  @DisplayName("covers(Instant) -> an active window covers every instant from its start (AAA)")
  void coversActiveWindow() {
    var window = buttonWindow();

    assertThat(window.covers(T0)).isTrue();
    assertThat(window.covers(T0.plusSeconds(86_400))).isTrue();
    assertThat(window.covers(T0.minusNanos(1))).isFalse();
  }

  @Test
  @DisplayName("covers(Instant) -> a closed window covers [start, end) only (AAA)")
  void coversClosedWindowIsHalfOpen() {
    var window = closedWindow();

    assertThat(window.covers(T0)).isTrue();
    assertThat(window.covers(window.getEndedAt().minusNanos(1))).isTrue();
    assertThat(window.covers(window.getEndedAt())).isFalse();
  }

  @Test
  @DisplayName("covers(Instant) -> a null instant is never covered (AAA)")
  void coversNull() {
    assertThat(buttonWindow().covers(null)).isFalse();
  }
}
