package pe.edu.upc.ice.track.platform.notifications.application.commandservices;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;

import java.util.Optional;

/** Application service handling write operations on {@link Notification}. */
public interface NotificationCommandService {

  /**
   *  Creates a new notification and persists it in the system.
   * @param command The command to Create a Notification
   * @return The created notification with its generated identifier.
   */
  Notification handle(CreateNotificationCommand command);

  /**
   *  Marks a notification as read and updates its state in the system.
   * @param command The command to mark a notification as read
   * @return  An {@code Optional} containing the updated notification if found, or empty if not found.
   */
  Optional<Notification> handle(MarkNotificationAsReadCommand command);

  /**
   *  Dismisses a notification and removes it from the recipient's active inbox.
   * @param command The command to dismiss a notification
   * @return  An {@code Optional} containing the dismissed notification if found, or empty if not found.
   */
  Optional<Notification> handle(DismissNotificationCommand command);
}
