package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.AlertId;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

class NotificationsCommandsTest {

  @Nested
  class CreateNotification {

    @Test
    @DisplayName("new CreateNotificationCommand(8 args) -> template is null (AAA)")
    void shortConstructorLeavesTemplateNull() {
      // Arrange & Act & Assert
      assertThat(createNotificationCommand().template()).isNull();
    }
  }

  @Nested
  class MarkAll {

    @Test
    @DisplayName("new MarkAllNotificationsAsReadCommand(recipient, null filter) -> defaults to NONE (AAA)")
    void nullFilterDefaultsToNone() {
      // Arrange & Act & Assert
      assertThat(new MarkAllNotificationsAsReadCommand(RECIPIENT_ID, null).filter()).isSameAs(NotificationFilter.NONE);
    }

    @Test
    @DisplayName("new MarkAllNotificationsAsReadCommand(null recipient, filter) -> IllegalArgumentException (AAA)")
    void rejectsNullRecipient() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new MarkAllNotificationsAsReadCommand(null, NotificationFilter.NONE))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class MarkSome {

    @Test
    @DisplayName("new MarkNotificationsAsReadCommand(recipient, ids) -> keeps an immutable copy of the ids (AAA)")
    void copiesIds() {
      // Arrange
      var ids = new HashSet<>(Set.of(1L, 2L));

      // Act
      var command = new MarkNotificationsAsReadCommand(RECIPIENT_ID, ids);
      ids.add(3L);

      // Assert
      assertThat(command.notificationIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("new MarkNotificationsAsReadCommand(null recipient or empty ids) -> IllegalArgumentException (AAA)")
    void rejectsMissingRecipientOrIds() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new MarkNotificationsAsReadCommand(null, Set.of(1L)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new MarkNotificationsAsReadCommand(RECIPIENT_ID, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new MarkNotificationsAsReadCommand(RECIPIENT_ID, Set.of()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new MarkNotificationsAsReadCommand(more than MAX_IDS ids) -> IllegalArgumentException (AAA)")
    void rejectsTooManyIds() {
      // Arrange
      var tooMany = LongStream.rangeClosed(1, MarkNotificationsAsReadCommand.MAX_IDS + 1L).boxed()
          .collect(Collectors.toSet());
      var maximum = LongStream.rangeClosed(1, MarkNotificationsAsReadCommand.MAX_IDS).boxed()
          .collect(Collectors.toSet());

      // Act & Assert
      assertThatThrownBy(() -> new MarkNotificationsAsReadCommand(RECIPIENT_ID, tooMany))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(new MarkNotificationsAsReadCommand(RECIPIENT_ID, maximum).notificationIds()).hasSize(100);
    }

    @Test
    @DisplayName("MarkNotificationsAsReadCommand.of(recipient, ids) -> drops nulls and duplicates (AAA)")
    void ofDeduplicatesAndDropsNulls() {
      // Arrange
      var ids = new ArrayList<>(Arrays.asList(1L, null, 1L, 2L));

      // Act
      var command = MarkNotificationsAsReadCommand.of(RECIPIENT_ID, ids);

      // Assert
      assertThat(command.notificationIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("MarkNotificationsAsReadCommand.of(recipient, null) -> IllegalArgumentException (AAA)")
    void ofRejectsNullList() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> MarkNotificationsAsReadCommand.of(RECIPIENT_ID, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> MarkNotificationsAsReadCommand.of(RECIPIENT_ID, List.of()))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class NotifyAlertRaised {

    @Test
    @DisplayName("new NotifyAlertRaisedCommand(5 args) -> silentMinutes is null (AAA)")
    void shortConstructorLeavesSilentMinutesNull() {
      // Arrange & Act & Assert
      assertThat(excursionAlertCommand(9.4).silentMinutes()).isNull();
    }

    @Test
    @DisplayName("new NotifyAlertRaisedCommand(missing required value) -> NullPointerException (AAA)")
    void rejectsMissingRequiredValues() {
      // Arrange
      var alertId = new AlertId(ALERT_ID);
      var equipmentId = new EquipmentId(EQUIPMENT_ID);

      // Act & Assert
      assertThatThrownBy(() -> new NotifyAlertRaisedCommand(null, equipmentId, NotificationType.DEVICE_OFFLINE,
          NotificationSeverity.WARNING, null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> new NotifyAlertRaisedCommand(alertId, null, NotificationType.DEVICE_OFFLINE,
          NotificationSeverity.WARNING, null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> new NotifyAlertRaisedCommand(alertId, equipmentId, null,
          NotificationSeverity.WARNING, null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> new NotifyAlertRaisedCommand(alertId, equipmentId, NotificationType.DEVICE_OFFLINE,
          null, null)).isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class MaintenanceWindowCommands {

    @Test
    @DisplayName("NotifyMaintenanceWindowOpenedCommand.unlinked() -> true only without service request (AAA)")
    void unlinkedDependsOnServiceRequest() {
      // Arrange & Act & Assert
      assertThat(windowOpenedCommand("APP", null).unlinked()).isTrue();
      assertThat(windowOpenedCommand("APP", SERVICE_REQUEST_ID).unlinked()).isFalse();
    }

    @Test
    @DisplayName("new NotifyMaintenanceWindowOpenedCommand(missing required value) -> IllegalArgumentException (AAA)")
    void openedRejectsMissingValues() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotifyMaintenanceWindowOpenedCommand(null, EQUIPMENT_ID, "APP", T0, T0, null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowOpenedCommand(WINDOW_ID, null, "APP", T0, T0, null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowOpenedCommand(WINDOW_ID, EQUIPMENT_ID, null, T0, T0, null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowOpenedCommand(WINDOW_ID, EQUIPMENT_ID, "APP", null, T0, null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowOpenedCommand(WINDOW_ID, EQUIPMENT_ID, "APP", T0, null, null, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new NotifyMaintenanceWindowClosedCommand(missing required value) -> IllegalArgumentException (AAA)")
    void closedRejectsMissingValues() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotifyMaintenanceWindowClosedCommand(null, EQUIPMENT_ID, "APP", T0, "EXPIRED", null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowClosedCommand(WINDOW_ID, null, "APP", T0, "EXPIRED", null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowClosedCommand(WINDOW_ID, EQUIPMENT_ID, null, T0, "EXPIRED", null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowClosedCommand(WINDOW_ID, EQUIPMENT_ID, "APP", null, "EXPIRED", null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyMaintenanceWindowClosedCommand(WINDOW_ID, EQUIPMENT_ID, "APP", T0, null, null, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class ServiceRequestCommands {

    @Test
    @DisplayName("new NotifyTechnicianAssignedCommand(missing required value) -> IllegalArgumentException (AAA)")
    void assignedRejectsMissingValues() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(null, TECHNICIAN_ID, null, EQUIPMENT_ID, SITE_ID, "REPAIR", "HIGH", T0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, null, null, EQUIPMENT_ID, SITE_ID, "REPAIR", "HIGH", T0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, null, SITE_ID, "REPAIR", "HIGH", T0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, EQUIPMENT_ID, null, "REPAIR", "HIGH", T0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, EQUIPMENT_ID, SITE_ID, null, "HIGH", T0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, EQUIPMENT_ID, SITE_ID, "REPAIR", null, T0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, EQUIPMENT_ID, SITE_ID, "REPAIR", "HIGH", (Instant) null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new NotifyTechnicianAssignedCommand(no previous technician) -> valid first assignment (AAA)")
    void assignedAllowsMissingPreviousTechnician() {
      // Arrange & Act
      var command = new NotifyTechnicianAssignedCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, EQUIPMENT_ID, SITE_ID,
          "REPAIR", "HIGH", T0);

      // Assert
      assertThat(command.previousTechnicianProfileId()).isNull();
    }

    @Test
    @DisplayName("new NotifyServiceRequestRejectedCommand(missing or blank value) -> IllegalArgumentException (AAA)")
    void rejectedRejectsMissingValues() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotifyServiceRequestRejectedCommand(null, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestRejectedCommand(SERVICE_REQUEST_ID, null, TECHNICIAN_ID, EQUIPMENT_ID, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestRejectedCommand(SERVICE_REQUEST_ID, OWNER_ID, null, EQUIPMENT_ID, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestRejectedCommand(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID, null, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestRejectedCommand(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestRejectedCommand(SERVICE_REQUEST_ID, OWNER_ID, TECHNICIAN_ID, EQUIPMENT_ID, "  "))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new NotifyServiceRequestCanceledCommand(missing or blank value) -> IllegalArgumentException (AAA)")
    void canceledRejectsMissingValues() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotifyServiceRequestCanceledCommand(null, TECHNICIAN_ID, EQUIPMENT_ID, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestCanceledCommand(SERVICE_REQUEST_ID, null, EQUIPMENT_ID, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestCanceledCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, null, "r"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestCanceledCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new NotifyServiceRequestCanceledCommand(SERVICE_REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, " "))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
