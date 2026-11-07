package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.assetmanagement.ExternalAssetManagementServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyAlertRaisedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.events.NotificationCreatedEvent;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;

import java.util.Optional;

@Service
@Slf4j
public class NotificationCommandServiceImpl implements NotificationCommandService {

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ExternalAssetManagementServiceForNotifications externalAssetManagementService;

  public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                        ApplicationEventPublisher eventPublisher,
                                        ExternalAssetManagementServiceForNotifications externalAssetManagementService) {
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
    this.externalAssetManagementService = externalAssetManagementService;
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
    return saved;
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
        command.severity())));
  }

  /**
   * Marks one of the caller's notifications as read. A notification addressed to anybody else is
   * answered as absent, so identifiers cannot be harvested.
   */
  @Override
  @Transactional
  public Optional<Notification> handle(MarkNotificationAsReadCommand command) {
    return findAddressedTo(command.notificationId(), command.recipientUserId()).map(notification -> {
      notification.markAsRead();
      return notificationRepository.save(notification);
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
      notification.dismiss();
      return notificationRepository.save(notification);
    });
  }

  private Optional<Notification> findAddressedTo(Long notificationId, Long recipientUserId) {
    return notificationRepository.findById(notificationId)
        .filter(notification -> notification.isAddressedTo(recipientUserId));
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
