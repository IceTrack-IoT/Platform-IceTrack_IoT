package pe.edu.upc.ice.track.platform.notifications.application.commandservices;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyAlertRaisedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowClosedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowOpenedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestCanceledCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestRejectedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyTechnicianAssignedCommand;

import java.util.List;
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
   *  Notifies the owner of a unit that an alert was raised on it.
   * @param command The command describing the raised alert
   * @return The created notification, or empty when no owner could be resolved for the unit
   */
  Optional<Notification> handle(NotifyAlertRaisedCommand command);

  /**
   *  Notifies a technician of a service request assigned to it, and the technician it replaced, if any.
   * @param command The command describing the assignment
   * @return The notifications created, the new technician's first
   */
  List<Notification> handle(NotifyTechnicianAssignedCommand command);

  /**
   *  Notifies an owner that the technician assigned to one of its requests rejected it.
   * @param command The command describing the rejection
   * @return The created notification
   */
  Notification handle(NotifyServiceRequestRejectedCommand command);

  /**
   *  Notifies the technician assigned to a request that its owner canceled it.
   * @param command The command describing the cancellation
   * @return The created notification
   */
  Notification handle(NotifyServiceRequestCanceledCommand command);

  /**
   *  Notifies the owner of a unit that a maintenance window opened on it; an unlinked window is
   *  notified as a warning.
   * @param command The command describing the window
   * @return The created notification, or empty when no owner could be resolved for the unit
   */
  Optional<Notification> handle(NotifyMaintenanceWindowOpenedCommand command);

  /**
   *  Notifies the owner of a unit that a maintenance window on it closed.
   * @param command The command describing the window
   * @return The created notification, or empty when no owner could be resolved for the unit
   */
  Optional<Notification> handle(NotifyMaintenanceWindowClosedCommand command);

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
