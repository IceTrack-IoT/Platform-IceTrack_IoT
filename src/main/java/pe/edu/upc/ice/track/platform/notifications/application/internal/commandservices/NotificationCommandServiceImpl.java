package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.application.internal.messages.NotificationMessageComposer;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl.ExternalAssetManagementServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl.ExternalProfilesServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkAllNotificationsAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationsAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyAlertRaisedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowClosedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowOpenedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestCanceledCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestRejectedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyTechnicianAssignedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.events.NotificationCreatedEvent;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.BulkReadResult;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.MessageTemplate;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.NotificationCreatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.UnreadNotificationCountChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Default {@link NotificationCommandService}.
 *
 * <p>Every notification is created with its English {@code message} and its template: a stable
 * {@code message_key} from the {@link MessageTemplate} catalog and the values a client fills into it
 * to show the text in the user's own language. Unit and technician names are read through the
 * outbound services when the notification is created, so a template never needs a second lookup on
 * the client.</p>
 */
@Service
@Slf4j
public class NotificationCommandServiceImpl implements NotificationCommandService {

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ExternalAssetManagementServiceForNotifications externalAssetManagementService;
  private final ExternalProfilesServiceForNotifications externalProfilesService;
  private final NotificationMessageComposer messageComposer;

  public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                        ApplicationEventPublisher eventPublisher,
                                        ExternalAssetManagementServiceForNotifications externalAssetManagementService,
                                        ExternalProfilesServiceForNotifications externalProfilesService,
                                        NotificationMessageComposer messageComposer) {
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
    this.externalAssetManagementService = externalAssetManagementService;
    this.externalProfilesService = externalProfilesService;
    this.messageComposer = messageComposer;
  }

  @Override
  public Notification handle(CreateNotificationCommand command) {
    var notification = new Notification(command);
    var saved = notificationRepository.save(notification);
    eventPublisher.publishEvent(new NotificationCreatedEvent(
        saved.getId(),
        saved.getRecipientUserId(),
        saved.getMessage(),
        saved.getType(),
        saved.getSeverity(),
        saved.getEquipmentId() == null ? null : saved.getEquipmentId().equipmentId(),
        saved.getSourceAlertId() == null ? null : saved.getSourceAlertId().alertId()));
    var unreadCount = notificationRepository.countActiveUnreadByRecipientUserId(saved.getRecipientUserId());
    eventPublisher.publishEvent(new NotificationCreatedIntegrationEvent(
        saved.getId(),
        saved.getRecipientUserId(),
        saved.getType().name(),
        saved.getSeverity().name(),
        saved.getEquipmentId() == null ? null : saved.getEquipmentId().equipmentId(),
        saved.getSourceAlertId() == null ? null : saved.getSourceAlertId().alertId(),
        saved.getCreatedAt(),
        unreadCount));
    announceUnreadCount(saved.getRecipientUserId(), unreadCount);
    return saved;
  }

  /**
   * Announces a recipient's new unread count to the other bounded contexts, so open screens update
   * their badge without asking for it.
   *
   * @param recipientUserId the recipient whose count changed
   * @param unreadCount     the count after the change
   */
  private void announceUnreadCount(Long recipientUserId, long unreadCount) {
    eventPublisher.publishEvent(new UnreadNotificationCountChangedIntegrationEvent(
        recipientUserId, unreadCount, Instant.now()));
  }

  /**
   * Announces the recipient's unread count when an operation changed it.
   *
   * @param recipientUserId the recipient
   * @param changed         whether the operation changed any notification from unread
   */
  private void announceUnreadCountIfChanged(Long recipientUserId, boolean changed) {
    if (changed) {
      announceUnreadCount(recipientUserId, notificationRepository.countActiveUnreadByRecipientUserId(recipientUserId));
    }
  }

  /**
   * Resolves the owner of the unit the alert was raised on and notifies them.
   *
   * <p>When the unit or its site no longer exists there is nobody to notify: a warning is logged,
   * so the dropped alert stays traceable, and nothing is created.</p>
   */
  @Override
  public Optional<Notification> handle(NotifyAlertRaisedCommand command) {
    var equipmentId = command.equipmentId().equipmentId();
    var recipientUserId = externalAssetManagementService.fetchEquipmentOwnerId(equipmentId);
    if (recipientUserId.isEmpty()) {
      log.warn("Unable to dispatch notification for alert {}: recipient user not found for equipment {} or its site.",
          command.alertId().alertId(), equipmentId);
      return Optional.empty();
    }
    var equipmentName = externalAssetManagementService.fetchEquipmentName(equipmentId).orElse(null);
    var breach = command.type() == NotificationType.TEMPERATURE_EXCURSION ? breachedLimit(command) : null;
    return Optional.of(handle(new CreateNotificationCommand(
        recipientUserId.get(),
        command.equipmentId(),
        null,
        command.alertId(),
        buildAlertMessage(command, equipmentName, breach),
        command.type(),
        command.severity(),
        null,
        alertTemplate(command, equipmentName, breach))));
  }

  /**
   * Finds which limit of its band a unit's excursion crossed, from the peak temperature.
   *
   * @param command the alert command
   * @return the limit crossed, or {@code null} when the peak or the band is unknown, or lies inside it
   */
  private BreachedLimit breachedLimit(NotifyAlertRaisedCommand command) {
    if (command.peakTemperature() == null) {
      return null;
    }
    var band = externalAssetManagementService.fetchThresholdRange(command.equipmentId().equipmentId()).orElse(null);
    if (band == null || band.length < 2) {
      return null;
    }
    if (command.peakTemperature() > band[1]) {
      return new BreachedLimit("MAX", band[1]);
    }
    if (command.peakTemperature() < band[0]) {
      return new BreachedLimit("MIN", band[0]);
    }
    return null;
  }

  /**
   * Builds the template of an alert notification.
   *
   * @param command       the alert command
   * @param equipmentName the unit's name, or {@code null} when unknown
   * @param breach        the limit an excursion crossed, or {@code null}
   * @return the template
   */
  private static MessageTemplate alertTemplate(NotifyAlertRaisedCommand command, String equipmentName, BreachedLimit breach) {
    var equipmentId = command.equipmentId().equipmentId();
    return switch (command.type()) {
      case TEMPERATURE_EXCURSION -> MessageTemplate.of(MessageTemplate.ALERT_TEMPERATURE_EXCURSION_RAISED)
          .with("equipment_id", equipmentId)
          .with("equipment_name", equipmentName)
          .with("temperature", command.peakTemperature())
          .with("limit", breach == null ? null : breach.limit())
          .with("threshold", breach == null ? null : breach.threshold())
          .build();
      case DEVICE_OFFLINE -> MessageTemplate.of(MessageTemplate.ALERT_DEVICE_OFFLINE_RAISED)
          .with("equipment_id", equipmentId)
          .with("equipment_name", equipmentName)
          .with("silent_minutes", command.silentMinutes())
          .build();
      default -> MessageTemplate.of(MessageTemplate.ALERT_SENSOR_FAILURE_RAISED)
          .with("equipment_id", equipmentId)
          .with("equipment_name", equipmentName)
          .build();
    };
  }

  /**
   * The limit of a band an excursion crossed.
   *
   * @param limit     {@code MAX} or {@code MIN}
   * @param threshold the value of that limit, in Celsius
   */
  private record BreachedLimit(String limit, double threshold) {
  }

  /**
   * Tells the new technician about the assignment, with the unit, site, priority and date, and the
   * technician it replaced, if any, that the request was reassigned.
   */
  @Override
  public List<Notification> handle(NotifyTechnicianAssignedCommand command) {
    var notifications = new ArrayList<Notification>();
    var equipmentName = equipmentNameOf(command.equipmentId());
    notifications.add(deliver(command.technicianProfileId(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.INFO,
        messageComposer.compose("notification.technician-assigned",
            command.serviceRequestId(), command.serviceType(), command.priority(), command.equipmentId(),
            command.siteId(), command.assignedAt()),
        MessageTemplate.of(MessageTemplate.SERVICE_REQUEST_ASSIGNED)
            .with("service_request_id", command.serviceRequestId())
            .with("equipment_name", equipmentName)
            .with("technician_name", externalProfilesService.fetchTechnicianFullName(command.technicianProfileId()).orElse(null))
            .build()));
    var previous = command.previousTechnicianProfileId();
    if (previous != null && !previous.equals(command.technicianProfileId())) {
      notifications.add(deliver(previous, command.equipmentId(), command.serviceRequestId(),
          NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.INFO,
          messageComposer.compose("notification.technician-unassigned",
              command.serviceRequestId(), command.equipmentId()),
          MessageTemplate.of(MessageTemplate.SERVICE_REQUEST_UNASSIGNED)
              .with("service_request_id", command.serviceRequestId())
              .with("equipment_name", equipmentName)
              .build()));
    }
    return notifications;
  }

  /** Tells the owner which technician rejected the request, and why. */
  @Override
  public Notification handle(NotifyServiceRequestRejectedCommand command) {
    return deliver(command.ownerProfileId(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.WARNING,
        messageComposer.compose("notification.service-request-rejected",
            command.technicianProfileId(), command.serviceRequestId(), command.equipmentId(), command.reason()),
        MessageTemplate.of(MessageTemplate.SERVICE_REQUEST_REJECTED)
            .with("service_request_id", command.serviceRequestId())
            .with("equipment_name", equipmentNameOf(command.equipmentId()))
            .with("reason", command.reason())
            .build());
  }

  /** Tells the technician that was assigned that the owner canceled the request, and why. */
  @Override
  public Notification handle(NotifyServiceRequestCanceledCommand command) {
    return deliver(command.technicianProfileId(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.WARNING,
        messageComposer.compose("notification.service-request-canceled",
            command.serviceRequestId(), command.equipmentId(), command.reason()),
        MessageTemplate.of(MessageTemplate.SERVICE_REQUEST_CANCELED)
            .with("service_request_id", command.serviceRequestId())
            .with("equipment_name", equipmentNameOf(command.equipmentId()))
            .with("reason", command.reason())
            .build());
  }

  /**
   * Tells the owner of the unit that a maintenance window opened, with its origin, its start and
   * expiration and, when known, its order and technician. A window with no order is flagged as
   * unlinked and sent as a warning.
   */
  @Override
  public Optional<Notification> handle(NotifyMaintenanceWindowOpenedCommand command) {
    var owner = externalAssetManagementService.fetchEquipmentOwnerId(command.equipmentId());
    if (owner.isEmpty()) {
      log.warn("Unable to notify maintenance window {}: no owner found for equipment {} or its site.",
          command.maintenanceWindowId(), command.equipmentId());
      return Optional.empty();
    }
    var origin = originLabel(command.origin());
    var template = MessageTemplate.of(MessageTemplate.MAINTENANCE_WINDOW_OPENED)
        .with("equipment_name", equipmentNameOf(command.equipmentId()))
        .with("origin", command.origin())
        .build();
    if (command.unlinked()) {
      return Optional.of(deliver(owner.get(), command.equipmentId(), null,
          NotificationType.UNLINKED_MAINTENANCE_WINDOW, NotificationSeverity.WARNING,
          messageComposer.compose("notification.maintenance-window-opened.unlinked",
              command.maintenanceWindowId(), command.equipmentId(), origin, command.startedAt(),
              command.expiresAt()),
          template));
    }
    return Optional.of(deliver(owner.get(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.MAINTENANCE_WINDOW, NotificationSeverity.INFO,
        messageComposer.compose("notification.maintenance-window-opened",
            command.maintenanceWindowId(), command.equipmentId(), origin, command.startedAt(), command.expiresAt(),
            command.serviceRequestId(), command.technicianProfileId()),
        template));
  }

  /** Tells the owner of the unit that a maintenance window closed, when and why. */
  @Override
  public Optional<Notification> handle(NotifyMaintenanceWindowClosedCommand command) {
    var owner = externalAssetManagementService.fetchEquipmentOwnerId(command.equipmentId());
    if (owner.isEmpty()) {
      log.warn("Unable to notify the end of maintenance window {}: no owner found for equipment {} or its site.",
          command.maintenanceWindowId(), command.equipmentId());
      return Optional.empty();
    }
    var origin = originLabel(command.origin());
    var message = command.serviceRequestId() == null
        ? messageComposer.compose("notification.maintenance-window-closed.unlinked",
            command.maintenanceWindowId(), command.equipmentId(), origin, command.endedAt(), command.closureReason())
        : messageComposer.compose("notification.maintenance-window-closed",
            command.maintenanceWindowId(), command.equipmentId(), origin, command.endedAt(), command.closureReason(),
            command.serviceRequestId(), command.technicianProfileId());
    var template = MessageTemplate.of(MessageTemplate.MAINTENANCE_WINDOW_CLOSED)
        .with("equipment_name", equipmentNameOf(command.equipmentId()))
        .with("closure_reason", command.closureReason())
        .build();
    return Optional.of(deliver(owner.get(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.MAINTENANCE_WINDOW, NotificationSeverity.INFO, message, template));
  }

  /**
   * Marks one of the caller's notifications as read. A notification addressed to anybody else is
   * answered as absent, so identifiers cannot be harvested.
   */
  @Override
  @Transactional
  public Optional<Notification> handle(MarkNotificationAsReadCommand command) {
    return findAddressedTo(command.notificationId(), command.recipientUserId()).map(notification -> {
      var wasUnread = isActiveAndUnread(notification);
      notification.markAsRead();
      var saved = notificationRepository.save(notification);
      announceUnreadCountIfChanged(command.recipientUserId(), wasUnread);
      return saved;
    });
  }

  /**
   * Dismisses one of the caller's notifications. A notification addressed to anybody else is
   * answered as absent.
   */
  @Override
  @Transactional
  public Optional<Notification> handle(DismissNotificationCommand command) {
    return findAddressedTo(command.notificationId(), command.recipientUserId()).map(notification -> {
      var wasUnread = isActiveAndUnread(notification);
      notification.dismiss();
      var saved = notificationRepository.save(notification);
      announceUnreadCountIfChanged(command.recipientUserId(), wasUnread);
      return saved;
    });
  }

  private static boolean isActiveAndUnread(Notification notification) {
    return !notification.isRead() && notification.getDismissedAt() == null;
  }

  /**
   * Marks every active, unread notification of the caller that matches the filter as read, in one
   * statement. Running it again updates nothing.
   */
  @Override
  @Transactional
  public BulkReadResult handle(MarkAllNotificationsAsReadCommand command) {
    var updated = notificationRepository.markActiveUnreadAsRead(
        command.recipientUserId(), command.filter(), Instant.now());
    var unread = notificationRepository.countActiveUnreadByRecipientUserId(command.recipientUserId());
    if (updated > 0) {
      announceUnreadCount(command.recipientUserId(), unread);
    }
    return new BulkReadResult(updated, unread);
  }

  /**
   * Marks the listed notifications of the caller as read, in one statement. An identifier addressed
   * to anybody else is skipped exactly like one already read, so the outcome reveals nothing about
   * other inboxes.
   */
  @Override
  @Transactional
  public BulkReadResult handle(MarkNotificationsAsReadCommand command) {
    var updated = notificationRepository.markActiveUnreadAsReadByIds(
        command.recipientUserId(), command.notificationIds(), Instant.now());
    var unread = notificationRepository.countActiveUnreadByRecipientUserId(command.recipientUserId());
    if (updated > 0) {
      announceUnreadCount(command.recipientUserId(), unread);
    }
    return new BulkReadResult(updated, unread);
  }

  private Optional<Notification> findAddressedTo(Long notificationId, Long recipientUserId) {
    return notificationRepository.findById(notificationId)
        .filter(notification -> notification.isAddressedTo(recipientUserId));
  }

  /**
   * Creates a notification that has no alert as its source.
   *
   * @param recipientProfileId the owner or technician profile it is addressed to
   * @param equipmentId        the unit it concerns, or {@code null}
   * @param serviceRequestId   the service request it is about, or {@code null} when it concerns none
   * @param type               the kind of notification
   * @param severity           its urgency
   * @param message            the English text shown to the recipient
   * @param template           the template key and values clients localize the text from
   * @return the stored notification
   */
  private Notification deliver(Long recipientProfileId, Long equipmentId, Long serviceRequestId,
                              NotificationType type, NotificationSeverity severity, String message,
                              MessageTemplate template) {
    return handle(new CreateNotificationCommand(
        recipientProfileId,
        equipmentId == null ? null : new EquipmentId(equipmentId),
        null,
        null,
        message,
        type,
        severity,
        serviceRequestId,
        template));
  }

  /**
   * Reads the name of a unit for a template.
   *
   * @param equipmentId the unit, or {@code null}
   * @return its name, or {@code null} when unknown
   */
  private String equipmentNameOf(Long equipmentId) {
    return externalAssetManagementService.fetchEquipmentName(equipmentId).orElse(null);
  }

  private static String originLabel(String origin) {
    return switch (origin) {
      case "APP" -> "app";
      case "BUTTON" -> "hardware button";
      default -> origin;
    };
  }

  /**
   * Composes the English text of an alert notification, naming the unit when its name is known.
   *
   * <p>Formatted with {@link Locale#ROOT}, so a decimal point is never written as a comma whatever the
   * server locale.</p>
   */
  private static String buildAlertMessage(NotifyAlertRaisedCommand command, String equipmentName, BreachedLimit breach) {
    var unit = equipmentName == null
        ? "Equipment %d".formatted(command.equipmentId().equipmentId())
        : equipmentName;
    if (breach != null) {
      return String.format(Locale.ROOT, "%s reached %.1f °C, %s its %s of %.1f °C",
          unit, command.peakTemperature(), "MAX".equals(breach.limit()) ? "above" : "below",
          "MAX".equals(breach.limit()) ? "maximum" : "minimum", breach.threshold());
    }
    if (command.type() == NotificationType.DEVICE_OFFLINE && command.silentMinutes() != null) {
      return "%s stopped reporting: no reading for %d minutes.".formatted(unit, command.silentMinutes());
    }
    if (command.peakTemperature() == null) {
      return "%s reported a new %s alert.".formatted(unit, command.type().name());
    }
    return String.format(Locale.ROOT, "%s reported a %s alert, peak temperature %.1f °C.",
        unit, command.type().name(), command.peakTemperature());
  }
}
