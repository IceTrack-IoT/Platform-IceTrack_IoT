package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;

import java.util.Optional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

  private final NotificationRepository notificationRepository;

  public NotificationCommandServiceImpl(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Override
  public Notification handle(CreateNotificationCommand command) {
    var notification = new Notification(command);
    return notificationRepository.save(notification);
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
