package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.notifications.application.internal.messages.NotificationMessageComposer;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl.ExternalAssetManagementServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl.ExternalProfilesServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.events.NotificationCreatedEvent;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.NotificationCreatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.UnreadNotificationCountChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceImplTest {

  private static final long UNREAD = 3L;

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private ExternalAssetManagementServiceForNotifications externalAssetManagementService;
  @Mock
  private ExternalProfilesServiceForNotifications externalProfilesService;
  @Mock
  private NotificationMessageComposer messageComposer;

  @InjectMocks
  private NotificationCommandServiceImpl service;

  private void givenRepositoryPersists() {
    lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
      Notification notification = invocation.getArgument(0);
      if (notification.getId() == null) {
        notification.assignId(NOTIFICATION_ID);
      }
      return notification;
    });
    lenient().when(notificationRepository.countActiveUnreadByRecipientUserId(anyLong())).thenReturn(UNREAD);
  }

  private List<Notification> savedNotifications() {
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository, atLeastOnce()).save(captor.capture());
    return captor.getAllValues();
  }

  private Notification onlySavedNotification() {
    var saved = savedNotifications();
    assertThat(saved).hasSize(1);
    return saved.get(0);
  }

  private List<Object> publishedEvents() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
    return captor.getAllValues();
  }

  private void givenOwnerAndName(Long ownerId) {
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(ownerId));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.of(EQUIPMENT_NAME));
  }

  @Test
  @DisplayName("handle(CreateNotificationCommand) -> saves it and announces creation, push and unread count (AAA)")
  void createSavesAndPublishesEvents() {
    // Arrange
    givenRepositoryPersists();

    // Act
    var result = service.handle(createNotificationCommand());

    // Assert
    assertThat(result.getId()).isEqualTo(NOTIFICATION_ID);
    assertThat(result.isRead()).isFalse();
    var events = publishedEvents();
    assertThat(events).hasSize(3);
    assertThat(events.get(0)).isEqualTo(new NotificationCreatedEvent(NOTIFICATION_ID, RECIPIENT_ID,
        result.getMessage(), NotificationType.TEMPERATURE_EXCURSION, NotificationSeverity.CRITICAL, EQUIPMENT_ID,
        ALERT_ID));
    assertThat(events.get(1)).isEqualTo(new NotificationCreatedIntegrationEvent(NOTIFICATION_ID, RECIPIENT_ID,
        "TEMPERATURE_EXCURSION", "CRITICAL", EQUIPMENT_ID, ALERT_ID, result.getCreatedAt(), UNREAD));
    var unreadEvent = (UnreadNotificationCountChangedIntegrationEvent) events.get(2);
    assertThat(unreadEvent.recipientProfileId()).isEqualTo(RECIPIENT_ID);
    assertThat(unreadEvent.unreadCount()).isEqualTo(UNREAD);
    assertThat(unreadEvent.changedAt()).isNotNull();
  }

  @Test
  @DisplayName("handle(CreateNotificationCommand without equipment or alert) -> events carry null ids (AAA)")
  void createWithoutOptionalReferences() {
    // Arrange
    givenRepositoryPersists();
    var command = new pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand(
        RECIPIENT_ID, null, null, null, "Plain message", NotificationType.MAINTENANCE_REMINDER,
        NotificationSeverity.INFO, null);

    // Act
    service.handle(command);

    // Assert
    var events = publishedEvents();
    var created = (NotificationCreatedEvent) events.get(0);
    assertThat(created.equipmentId()).isNull();
    assertThat(created.sourceAlertId()).isNull();
    var integration = (NotificationCreatedIntegrationEvent) events.get(1);
    assertThat(integration.equipmentId()).isNull();
    assertThat(integration.sourceAlertId()).isNull();
  }

  @Test
  @DisplayName("handle(NotifyAlertRaisedCommand) without owner -> empty and nothing saved (AAA)")
  void alertWithoutOwnerIsSkipped() {
    // Arrange
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(excursionAlertCommand(9.4));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(notificationRepository, eventPublisher);
  }

  @Test
  @DisplayName("handle(excursion above the maximum) -> message and template name the MAX limit (AAA)")
  void excursionAboveMaximum() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));

    // Act
    var result = service.handle(excursionAlertCommand(9.4));

    // Assert
    assertThat(result).isPresent();
    var saved = onlySavedNotification();
    assertThat(saved.getRecipientUserId()).isEqualTo(RECIPIENT_ID);
    assertThat(saved.getMessage()).isEqualTo("Cold Room #01 reached 9.4 °C, above its maximum of 8.0 °C");
    assertThat(saved.getType()).isEqualTo(NotificationType.TEMPERATURE_EXCURSION);
    assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
    assertThat(saved.getSourceAlertId()).isEqualTo(new AlertId(ALERT_ID));
    assertThat(saved.getEquipmentId()).isEqualTo(new EquipmentId(EQUIPMENT_ID));
    assertThat(saved.getTemplate().key()).isEqualTo(MessageTemplate.ALERT_TEMPERATURE_EXCURSION_RAISED);
    assertThat(saved.getTemplate().params())
        .containsEntry("equipment_id", EQUIPMENT_ID)
        .containsEntry("equipment_name", EQUIPMENT_NAME)
        .containsEntry("temperature", 9.4)
        .containsEntry("limit", "MAX")
        .containsEntry("threshold", 8.0);
    verifyNoInteractions(messageComposer);
  }

  @Test
  @DisplayName("handle(excursion below the minimum) -> message and template name the MIN limit (AAA)")
  void excursionBelowMinimum() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));

    // Act
    service.handle(excursionAlertCommand(1.0));

    // Assert
    var saved = onlySavedNotification();
    assertThat(saved.getMessage()).isEqualTo("Cold Room #01 reached 1.0 °C, below its minimum of 2.0 °C");
    assertThat(saved.getTemplate().params()).containsEntry("limit", "MIN").containsEntry("threshold", 2.0);
  }

  @Test
  @DisplayName("handle(excursion inside the band) -> generic alert message without limit (AAA)")
  void excursionInsideBandHasNoBreach() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));

    // Act
    service.handle(excursionAlertCommand(5.0));

    // Assert
    var saved = onlySavedNotification();
    assertThat(saved.getMessage())
        .isEqualTo("Cold Room #01 reported a TEMPERATURE_EXCURSION alert, peak temperature 5.0 °C.");
    assertThat(saved.getTemplate().params()).doesNotContainKeys("limit", "threshold");
  }

  @Test
  @DisplayName("handle(excursion with unknown or malformed threshold) -> no breach is reported (AAA)")
  void excursionWithoutUsableThreshold() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    service.handle(excursionAlertCommand(9.4));

    // Assert
    assertThat(onlySavedNotification().getTemplate().params()).doesNotContainKeys("limit", "threshold");
  }

  @Test
  @DisplayName("handle(excursion with a one-value threshold) -> no breach is reported (AAA)")
  void excursionWithShortThreshold() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);
    when(externalAssetManagementService.fetchThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {8.0}));

    // Act
    service.handle(excursionAlertCommand(9.4));

    // Assert
    assertThat(onlySavedNotification().getTemplate().params()).doesNotContainKeys("limit", "threshold");
  }

  @Test
  @DisplayName("handle(excursion without peak temperature) -> asks no threshold and says a new alert was reported (AAA)")
  void excursionWithoutPeak() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);

    // Act
    service.handle(excursionAlertCommand(null));

    // Assert
    var saved = onlySavedNotification();
    assertThat(saved.getMessage()).isEqualTo("Cold Room #01 reported a new TEMPERATURE_EXCURSION alert.");
    assertThat(saved.getTemplate().params()).doesNotContainKey("temperature");
    verify(externalAssetManagementService, never()).fetchThresholdRange(any());
  }

  @Test
  @DisplayName("handle(alert of an unnamed equipment) -> falls back to 'Equipment <id>' and omits the name param (AAA)")
  void alertWithoutEquipmentName() {
    // Arrange
    givenRepositoryPersists();
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(RECIPIENT_ID));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    service.handle(sensorFailureAlertCommand());

    // Assert
    var saved = onlySavedNotification();
    assertThat(saved.getMessage()).isEqualTo("Equipment 10 reported a new SENSOR_FAILURE alert.");
    assertThat(saved.getTemplate().key()).isEqualTo(MessageTemplate.ALERT_SENSOR_FAILURE_RAISED);
    assertThat(saved.getTemplate().params()).containsOnlyKeys("equipment_id");
  }

  @Test
  @DisplayName("handle(offline alert with silent minutes) -> 'stopped reporting' message and offline template (AAA)")
  void offlineAlertWithSilence() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);

    // Act
    service.handle(offlineAlertCommand(5L));

    // Assert
    var saved = onlySavedNotification();
    assertThat(saved.getType()).isEqualTo(NotificationType.DEVICE_OFFLINE);
    assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
    assertThat(saved.getMessage()).isEqualTo("Cold Room #01 stopped reporting: no reading for 5 minutes.");
    assertThat(saved.getTemplate().key()).isEqualTo(MessageTemplate.ALERT_DEVICE_OFFLINE_RAISED);
    assertThat(saved.getTemplate().params()).containsEntry("silent_minutes", 5L);
    verify(externalAssetManagementService, never()).fetchThresholdRange(any());
  }

  @Test
  @DisplayName("handle(offline alert without silent minutes) -> generic alert message (AAA)")
  void offlineAlertWithoutSilence() {
    // Arrange
    givenRepositoryPersists();
    givenOwnerAndName(RECIPIENT_ID);

    // Act
    service.handle(offlineAlertCommand(null));

    // Assert
    var saved = onlySavedNotification();
    assertThat(saved.getMessage()).isEqualTo("Cold Room #01 reported a new DEVICE_OFFLINE alert.");
    assertThat(saved.getTemplate().params()).doesNotContainKey("silent_minutes");
  }

  @Test
  @DisplayName("handle(NotifyTechnicianAssignedCommand first assignment) -> one INFO notification to the technician (AAA)")
  void firstAssignmentNotifiesTechnician() {
    // Arrange
    givenRepositoryPersists();
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.of(EQUIPMENT_NAME));
    when(externalProfilesService.fetchTechnicianFullName(TECHNICIAN_ID)).thenReturn(Optional.of("Ana Torres"));
    doReturn("You were assigned").when(messageComposer).compose("notification.technician-assigned",
        SERVICE_REQUEST_ID, "REPAIR", "HIGH", EQUIPMENT_ID, SITE_ID, T0);

    // Act
    var result = service.handle(technicianAssignedCommand(null));

    // Assert
    assertThat(result).hasSize(1);
    var saved = onlySavedNotification();
    assertThat(saved.getRecipientUserId()).isEqualTo(TECHNICIAN_ID);
    assertThat(saved.getMessage()).isEqualTo("You were assigned");
    assertThat(saved.getType()).isEqualTo(NotificationType.SERVICE_REQUEST_UPDATE);
    assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.INFO);
    assertThat(saved.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(saved.getEquipmentId()).isEqualTo(new EquipmentId(EQUIPMENT_ID));
    assertThat(saved.getTemplate().key()).isEqualTo(MessageTemplate.SERVICE_REQUEST_ASSIGNED);
    assertThat(saved.getTemplate().params())
        .containsEntry("service_request_id", SERVICE_REQUEST_ID)
        .containsEntry("equipment_name", EQUIPMENT_NAME)
        .containsEntry("technician_name", "Ana Torres");
  }

  @Test
  @DisplayName("handle(NotifyTechnicianAssignedCommand reassignment) -> also tells the previous technician (AAA)")
  void reassignmentAlsoNotifiesPreviousTechnician() {
    // Arrange
    givenRepositoryPersists();
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(externalProfilesService.fetchTechnicianFullName(TECHNICIAN_ID)).thenReturn(Optional.empty());
    doReturn("assigned").when(messageComposer).compose("notification.technician-assigned",
        SERVICE_REQUEST_ID, "REPAIR", "HIGH", EQUIPMENT_ID, SITE_ID, T0);
    doReturn("unassigned").when(messageComposer).compose("notification.technician-unassigned",
        SERVICE_REQUEST_ID, EQUIPMENT_ID);

    // Act
    var result = service.handle(technicianReassignedCommand());

    // Assert
    assertThat(result).hasSize(2);
    var saved = savedNotifications();
    assertThat(saved).extracting(Notification::getRecipientUserId).containsExactly(TECHNICIAN_ID, PREVIOUS_TECHNICIAN_ID);
    assertThat(saved.get(1).getMessage()).isEqualTo("unassigned");
    assertThat(saved.get(1).getTemplate().key()).isEqualTo(MessageTemplate.SERVICE_REQUEST_UNASSIGNED);
    assertThat(saved.get(1).getTemplate().params()).containsOnlyKeys("service_request_id");
    assertThat(saved.get(0).getTemplate().params()).containsOnlyKeys("service_request_id");
  }

  @Test
  @DisplayName("handle(NotifyTechnicianAssignedCommand to the same technician) -> only one notification (AAA)")
  void sameTechnicianIsNotNotifiedTwice() {
    // Arrange
    givenRepositoryPersists();
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(externalProfilesService.fetchTechnicianFullName(TECHNICIAN_ID)).thenReturn(Optional.empty());
    doReturn("assigned").when(messageComposer).compose("notification.technician-assigned",
        SERVICE_REQUEST_ID, "REPAIR", "HIGH", EQUIPMENT_ID, SITE_ID, T0);

    // Act
    var result = service.handle(technicianAssignedCommand(TECHNICIAN_ID));

    // Assert
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("handle(NotifyServiceRequestRejectedCommand) -> WARNING notification to the owner with the reason (AAA)")
  void rejectedNotifiesOwner() {
    // Arrange
    givenRepositoryPersists();
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.of(EQUIPMENT_NAME));
    doReturn("rejected text").when(messageComposer).compose("notification.service-request-rejected",
        TECHNICIAN_ID, SERVICE_REQUEST_ID, EQUIPMENT_ID, "Not enough spare parts");

    // Act
    var result = service.handle(serviceRequestRejectedCommand());

    // Assert
    assertThat(result.getRecipientUserId()).isEqualTo(OWNER_ID);
    assertThat(result.getMessage()).isEqualTo("rejected text");
    assertThat(result.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
    assertThat(result.getType()).isEqualTo(NotificationType.SERVICE_REQUEST_UPDATE);
    assertThat(result.getTemplate().key()).isEqualTo(MessageTemplate.SERVICE_REQUEST_REJECTED);
    assertThat(result.getTemplate().params())
        .containsEntry("reason", "Not enough spare parts")
        .containsEntry("equipment_name", EQUIPMENT_NAME)
        .containsEntry("service_request_id", SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("handle(NotifyServiceRequestCanceledCommand) -> WARNING notification to the technician (AAA)")
  void canceledNotifiesTechnician() {
    // Arrange
    givenRepositoryPersists();
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.empty());
    doReturn("canceled text").when(messageComposer).compose("notification.service-request-canceled",
        SERVICE_REQUEST_ID, EQUIPMENT_ID, "Owner canceled");

    // Act
    var result = service.handle(serviceRequestCanceledCommand());

    // Assert
    assertThat(result.getRecipientUserId()).isEqualTo(TECHNICIAN_ID);
    assertThat(result.getMessage()).isEqualTo("canceled text");
    assertThat(result.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
    assertThat(result.getTemplate().key()).isEqualTo(MessageTemplate.SERVICE_REQUEST_CANCELED);
    assertThat(result.getTemplate().params()).doesNotContainKey("equipment_name");
  }

  // ---------- maintenance windows ----------

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowOpenedCommand without owner) -> empty (AAA)")
  void windowOpenedWithoutOwner() {
    // Arrange
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(windowOpenedCommand("APP", SERVICE_REQUEST_ID));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(notificationRepository, messageComposer);
  }

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowOpenedCommand linked) -> INFO MAINTENANCE_WINDOW for the owner (AAA)")
  void windowOpenedLinked() {
    // Arrange
    givenRepositoryPersists();
    var command = windowOpenedCommand("APP", SERVICE_REQUEST_ID);
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.of(EQUIPMENT_NAME));
    doReturn("opened text").when(messageComposer).compose("notification.maintenance-window-opened",
        WINDOW_ID, EQUIPMENT_ID, "app", command.startedAt(), command.expiresAt(), SERVICE_REQUEST_ID, TECHNICIAN_ID);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(result).isPresent();
    var saved = result.get();
    assertThat(saved.getRecipientUserId()).isEqualTo(OWNER_ID);
    assertThat(saved.getType()).isEqualTo(NotificationType.MAINTENANCE_WINDOW);
    assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.INFO);
    assertThat(saved.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(saved.getMessage()).isEqualTo("opened text");
    assertThat(saved.getTemplate().key()).isEqualTo(MessageTemplate.MAINTENANCE_WINDOW_OPENED);
    assertThat(saved.getTemplate().params()).containsEntry("origin", "APP").containsEntry("equipment_name", EQUIPMENT_NAME);
  }

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowOpenedCommand unlinked, BUTTON) -> WARNING UNLINKED_MAINTENANCE_WINDOW (AAA)")
  void windowOpenedUnlinked() {
    // Arrange
    givenRepositoryPersists();
    var command = windowOpenedCommand("BUTTON", null);
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.of(EQUIPMENT_NAME));
    doReturn("unlinked text").when(messageComposer).compose("notification.maintenance-window-opened.unlinked",
        WINDOW_ID, EQUIPMENT_ID, "hardware button", command.startedAt(), command.expiresAt());

    // Act
    var result = service.handle(command);

    // Assert
    var saved = result.orElseThrow();
    assertThat(saved.getType()).isEqualTo(NotificationType.UNLINKED_MAINTENANCE_WINDOW);
    assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
    assertThat(saved.getServiceRequestId()).isNull();
    assertThat(saved.getMessage()).isEqualTo("unlinked text");
    assertThat(saved.getTemplate().params()).containsEntry("origin", "BUTTON");
  }

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowOpenedCommand unknown origin) -> origin text is passed through (AAA)")
  void windowOpenedUnknownOriginPassesThrough() {
    // Arrange
    givenRepositoryPersists();
    var command = windowOpenedCommand("REMOTE", null);
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.empty());
    doReturn("text").when(messageComposer).compose("notification.maintenance-window-opened.unlinked",
        WINDOW_ID, EQUIPMENT_ID, "REMOTE", command.startedAt(), command.expiresAt());

    // Act & Assert
    assertThat(service.handle(command)).isPresent();
  }

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowClosedCommand without owner) -> empty (AAA)")
  void windowClosedWithoutOwner() {
    // Arrange
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(windowClosedCommand("APP", SERVICE_REQUEST_ID));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(notificationRepository);
  }

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowClosedCommand linked) -> INFO notification with order and technician (AAA)")
  void windowClosedLinked() {
    // Arrange
    givenRepositoryPersists();
    var command = windowClosedCommand("APP", SERVICE_REQUEST_ID);
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.of(EQUIPMENT_NAME));
    doReturn("closed text").when(messageComposer).compose("notification.maintenance-window-closed",
        WINDOW_ID, EQUIPMENT_ID, "app", command.endedAt(), "MANUAL_APP", SERVICE_REQUEST_ID, TECHNICIAN_ID);

    // Act
    var saved = service.handle(command).orElseThrow();

    // Assert
    assertThat(saved.getRecipientUserId()).isEqualTo(OWNER_ID);
    assertThat(saved.getType()).isEqualTo(NotificationType.MAINTENANCE_WINDOW);
    assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.INFO);
    assertThat(saved.getMessage()).isEqualTo("closed text");
    assertThat(saved.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(saved.getTemplate().key()).isEqualTo(MessageTemplate.MAINTENANCE_WINDOW_CLOSED);
    assertThat(saved.getTemplate().params()).containsEntry("closure_reason", "MANUAL_APP");
  }

  @Test
  @DisplayName("handle(NotifyMaintenanceWindowClosedCommand unlinked) -> uses the unlinked message (AAA)")
  void windowClosedUnlinked() {
    // Arrange
    givenRepositoryPersists();
    var command = windowClosedCommand("BUTTON", null);
    when(externalAssetManagementService.fetchEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));
    when(externalAssetManagementService.fetchEquipmentName(EQUIPMENT_ID)).thenReturn(Optional.empty());
    doReturn("closed unlinked").when(messageComposer).compose("notification.maintenance-window-closed.unlinked",
        WINDOW_ID, EQUIPMENT_ID, "hardware button", command.endedAt(), "MANUAL_APP");

    // Act
    var saved = service.handle(command).orElseThrow();

    // Assert
    assertThat(saved.getMessage()).isEqualTo("closed unlinked");
    assertThat(saved.getServiceRequestId()).isNull();
  }

  @Test
  @DisplayName("handle(MarkNotificationAsReadCommand) of an unread notification -> read, saved and count announced (AAA)")
  void markAsReadUnread() {
    // Arrange
    givenRepositoryPersists();
    var notification = persistedNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    var result = service.handle(markAsReadCommand());

    // Assert
    assertThat(result).containsSame(notification);
    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getReadAt()).isNotNull();
    verify(notificationRepository, times(1)).save(notification);
    var events = publishedEvents();
    assertThat(events).hasSize(1);
    var unread = (UnreadNotificationCountChangedIntegrationEvent) events.get(0);
    assertThat(unread.recipientProfileId()).isEqualTo(RECIPIENT_ID);
    assertThat(unread.unreadCount()).isEqualTo(UNREAD);
  }

  @Test
  @DisplayName("handle(MarkNotificationAsReadCommand) of an already read notification -> saved, no announcement (AAA)")
  void markAsReadAlreadyRead() {
    // Arrange
    givenRepositoryPersists();
    var notification = readNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    var result = service.handle(markAsReadCommand());

    // Assert
    assertThat(result).isPresent();
    verify(notificationRepository, times(1)).save(notification);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("handle(MarkNotificationAsReadCommand) of a dismissed unread notification -> no announcement (AAA)")
  void markAsReadDismissedUnread() {
    // Arrange
    givenRepositoryPersists();
    var notification = dismissedNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    service.handle(markAsReadCommand());

    // Assert
    assertThat(notification.isRead()).isTrue();
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("handle(MarkNotificationAsReadCommand) unknown id -> empty (AAA)")
  void markAsReadUnknown() {
    // Arrange
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(service.handle(markAsReadCommand())).isEmpty();
    verify(notificationRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(MarkNotificationAsReadCommand) of another recipient's notification -> empty, untouched (AAA)")
  void markAsReadOfSomeoneElse() {
    // Arrange
    var notification = persistedNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
    var command = new pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand(
        NOTIFICATION_ID, OTHER_RECIPIENT_ID);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(result).isEmpty();
    assertThat(notification.isRead()).isFalse();
    verify(notificationRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(DismissNotificationCommand) of an unread notification -> dismissed and count announced (AAA)")
  void dismissUnread() {
    // Arrange
    givenRepositoryPersists();
    var notification = persistedNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    var result = service.handle(dismissCommand());

    // Assert
    assertThat(result).containsSame(notification);
    assertThat(notification.getDismissedAt()).isNotNull();
    verify(notificationRepository, times(1)).save(notification);
    assertThat(publishedEvents()).hasSize(1).first().isInstanceOf(UnreadNotificationCountChangedIntegrationEvent.class);
  }

  @Test
  @DisplayName("handle(DismissNotificationCommand) of a read notification -> dismissed, no announcement (AAA)")
  void dismissRead() {
    // Arrange
    givenRepositoryPersists();
    var notification = readNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    service.handle(dismissCommand());

    // Assert
    assertThat(notification.getDismissedAt()).isNotNull();
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("handle(DismissNotificationCommand) of an already dismissed notification -> idempotent, no announcement (AAA)")
  void dismissAlreadyDismissed() {
    // Arrange
    givenRepositoryPersists();
    var notification = dismissedNotification();
    var firstDismissal = notification.getDismissedAt();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    service.handle(dismissCommand());

    // Assert
    assertThat(notification.getDismissedAt()).isEqualTo(firstDismissal);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("handle(DismissNotificationCommand) of another recipient's notification -> empty (AAA)")
  void dismissOfSomeoneElse() {
    // Arrange
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(persistedNotification()));
    var command = new pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand(
        NOTIFICATION_ID, OTHER_RECIPIENT_ID);

    // Act & Assert
    assertThat(service.handle(command)).isEmpty();
    verify(notificationRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(MarkAllNotificationsAsReadCommand) updating some -> result and announcement (AAA)")
  void markAllUpdatesAndAnnounces() {
    // Arrange
    when(notificationRepository.markActiveUnreadAsRead(eqRecipient(), eqFilter(), any(Instant.class))).thenReturn(4L);
    when(notificationRepository.countActiveUnreadByRecipientUserId(RECIPIENT_ID)).thenReturn(1L);

    // Act
    var result = service.handle(markAllAsReadCommand());

    // Assert
    assertThat(result).isEqualTo(new BulkReadResult(4L, 1L));
    var events = publishedEvents();
    assertThat(events).hasSize(1);
    assertThat(((UnreadNotificationCountChangedIntegrationEvent) events.get(0)).unreadCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("handle(MarkAllNotificationsAsReadCommand) updating none -> result without announcement (AAA)")
  void markAllUpdatesNothing() {
    // Arrange
    when(notificationRepository.markActiveUnreadAsRead(eqRecipient(), eqFilter(), any(Instant.class))).thenReturn(0L);
    when(notificationRepository.countActiveUnreadByRecipientUserId(RECIPIENT_ID)).thenReturn(2L);

    // Act
    var result = service.handle(markAllAsReadCommand());

    // Assert
    assertThat(result).isEqualTo(new BulkReadResult(0L, 2L));
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("handle(MarkNotificationsAsReadCommand) updating some -> result and announcement (AAA)")
  void markSomeUpdatesAndAnnounces() {
    // Arrange
    when(notificationRepository.markActiveUnreadAsReadByIds(org.mockito.ArgumentMatchers.eq(RECIPIENT_ID),
        org.mockito.ArgumentMatchers.eq(Set.of(NOTIFICATION_ID, 13L)), any(Instant.class))).thenReturn(2L);
    when(notificationRepository.countActiveUnreadByRecipientUserId(RECIPIENT_ID)).thenReturn(0L);

    // Act
    var result = service.handle(markSomeAsReadCommand());

    // Assert
    assertThat(result).isEqualTo(new BulkReadResult(2L, 0L));
    assertThat(publishedEvents()).hasSize(1);
  }

  @Test
  @DisplayName("handle(MarkNotificationsAsReadCommand) updating none -> no announcement (AAA)")
  void markSomeUpdatesNothing() {
    // Arrange
    when(notificationRepository.markActiveUnreadAsReadByIds(org.mockito.ArgumentMatchers.eq(RECIPIENT_ID),
        org.mockito.ArgumentMatchers.eq(Set.of(NOTIFICATION_ID, 13L)), any(Instant.class))).thenReturn(0L);
    when(notificationRepository.countActiveUnreadByRecipientUserId(RECIPIENT_ID)).thenReturn(5L);

    // Act
    var result = service.handle(markSomeAsReadCommand());

    // Assert
    assertThat(result).isEqualTo(new BulkReadResult(0L, 5L));
    verifyNoInteractions(eventPublisher);
  }

  private static Long eqRecipient() {
    return org.mockito.ArgumentMatchers.eq(RECIPIENT_ID);
  }

  private static NotificationFilter eqFilter() {
    return org.mockito.ArgumentMatchers.eq(NotificationFilter.NONE);
  }
}
