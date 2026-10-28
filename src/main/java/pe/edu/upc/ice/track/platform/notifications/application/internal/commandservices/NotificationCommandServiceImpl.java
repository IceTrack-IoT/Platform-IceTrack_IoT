package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.events.NotificationCreatedEvent;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;

import java.util.Optional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                        ApplicationEventPublisher eventPublisher) {
    this.notificationRepository = notificationRepository;
    this.eventPublisher=eventPublisher;
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

  @Override
  public Optional<Notification> handle(MarkNotificationAsReadCommand command) {
    return notificationRepository.findById(command.notificationId()).map(notification -> {
      notification.markAsRead();
      return notificationRepository.save(notification);
    });
  }

  @Override
  public Optional<Notification> handle(DismissNotificationCommand command) {
    return notificationRepository.findById(command.notificationId()).map(notification -> {
      notification.dismiss();
      return notificationRepository.save(notification);
    });
  }
}
