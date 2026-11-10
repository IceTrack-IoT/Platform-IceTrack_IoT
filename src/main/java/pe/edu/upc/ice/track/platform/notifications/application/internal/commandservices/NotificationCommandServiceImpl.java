package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.application.internal.messages.NotificationMessageComposer;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl.ExternalAssetManagementServiceForNotifications;
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
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.NotificationCreatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.interfaces.events.UnreadNotificationCountChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class NotificationCommandServiceImpl implements NotificationCommandService {

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ExternalAssetManagementServiceForNotifications externalAssetManagementService;
  private final NotificationMessageComposer messageComposer;

  public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                        ApplicationEventPublisher eventPublisher,
                                        ExternalAssetManagementServiceForNotifications externalAssetManagementService,
                                        NotificationMessageComposer messageComposer) {
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
    this.externalAssetManagementService = externalAssetManagementService;
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
    return Optional.of(handle(new CreateNotificationCommand(
        recipientUserId.get(),
        command.equipmentId(),
        null,
        command.alertId(),
        buildAlertMessage(command),
        command.type(),
        command.severity(),
        null)));
  }

  /**
   * Tells the new technician about the assignment, with the unit, site, priority and date, and the
   * technician it replaced, if any, that the request was reassigned.
   */
  @Override
  public List<Notification> handle(NotifyTechnicianAssignedCommand command) {
    var notifications = new ArrayList<Notification>();
    notifications.add(deliver(command.technicianProfileId(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.INFO,
        messageComposer.compose("notification.technician-assigned",
            command.serviceRequestId(), command.serviceType(), command.priority(), command.equipmentId(),
            command.siteId(), command.assignedAt())));
    var previous = command.previousTechnicianProfileId();
    if (previous != null && !previous.equals(command.technicianProfileId())) {
      notifications.add(deliver(previous, command.equipmentId(), command.serviceRequestId(),
          NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.INFO,
          messageComposer.compose("notification.technician-unassigned",
              command.serviceRequestId(), command.equipmentId())));
    }
    return notifications;
  }

  /** Tells the owner which technician rejected the request, and why. */
  @Override
  public Notification handle(NotifyServiceRequestRejectedCommand command) {
    return deliver(command.ownerProfileId(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.WARNING,
        messageComposer.compose("notification.service-request-rejected",
            command.technicianProfileId(), command.serviceRequestId(), command.equipmentId(), command.reason()));
  }

  /** Tells the technician that was assigned that the owner canceled the request, and why. */
  @Override
  public Notification handle(NotifyServiceRequestCanceledCommand command) {
    return deliver(command.technicianProfileId(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.SERVICE_REQUEST_UPDATE, NotificationSeverity.WARNING,
        messageComposer.compose("notification.service-request-canceled",
            command.serviceRequestId(), command.equipmentId(), command.reason()));
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
    if (command.unlinked()) {
      return Optional.of(deliver(owner.get(), command.equipmentId(), null,
          NotificationType.UNLINKED_MAINTENANCE_WINDOW, NotificationSeverity.WARNING,
          messageComposer.compose("notification.maintenance-window-opened.unlinked",
              command.maintenanceWindowId(), command.equipmentId(), origin, command.startedAt(),
              command.expiresAt())));
    }
    return Optional.of(deliver(owner.get(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.MAINTENANCE_WINDOW, NotificationSeverity.INFO,
        messageComposer.compose("notification.maintenance-window-opened",
            command.maintenanceWindowId(), command.equipmentId(), origin, command.startedAt(), command.expiresAt(),
            command.serviceRequestId(), command.technicianProfileId())));
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
    return Optional.of(deliver(owner.get(), command.equipmentId(), command.serviceRequestId(),
        NotificationType.MAINTENANCE_WINDOW, NotificationSeverity.INFO, message));
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
   * @param message            the text shown to the recipient
   * @return the stored notification
   */
  private Notification deliver(Long recipientProfileId, Long equipmentId, Long serviceRequestId,
                              NotificationType type, NotificationSeverity severity, String message) {
    return handle(new CreateNotificationCommand(
        recipientProfileId,
        equipmentId == null ? null : new EquipmentId(equipmentId),
        null,
        null,
        message,
        type,
        severity,
        serviceRequestId));
  }

  private static String originLabel(String origin) {
    return switch (origin) {
      case "APP" -> "app";
      case "BUTTON" -> "hardware button";
      default -> origin;
    };
  }

  private static String buildAlertMessage(NotifyAlertRaisedCommand command) {
    var equipmentId = command.equipmentId().equipmentId();
    if (command.peakTemperature() == null) {
      return "Equipment %d reported a new %s alert.".formatted(equipmentId, command.type().name());
    }
    return "Equipment %d reported a %s alert, peak temperature %.1f C."
        .formatted(equipmentId, command.type().name(), command.peakTemperature());
  }
}
