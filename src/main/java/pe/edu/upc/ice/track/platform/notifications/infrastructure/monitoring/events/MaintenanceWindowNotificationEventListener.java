package pe.edu.upc.ice.track.platform.notifications.infrastructure.monitoring.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowClosedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowOpenedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowClosedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowOpenedCommand;

/**
 * Inbound infrastructure adapter that subscribes to the maintenance window integration events of
 * Monitoring and Alerting and notifies the owner of the unit.
 *
 * <p>Runs on its own thread once the window's transaction committed, so neither a slow notification
 * nor a failing one affects the gateway event that opened or closed the window. Failures are logged
 * and swallowed. A window opened with no service request is notified at once as an unlinked
 * maintenance window, so the owner can check who is working on the unit.</p>
 */
@Component
@Slf4j
public class MaintenanceWindowNotificationEventListener {

  private final NotificationCommandService notificationCommandService;

  public MaintenanceWindowNotificationEventListener(NotificationCommandService notificationCommandService) {
    this.notificationCommandService = notificationCommandService;
  }

  /**
   * @param event the integration event published when a window opens
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(MaintenanceWindowOpenedIntegrationEvent event) {
    try {
      notificationCommandService.handle(new NotifyMaintenanceWindowOpenedCommand(
          event.maintenanceWindowId(),
          event.equipmentId(),
          event.origin(),
          event.startedAt(),
          event.expiresAt(),
          event.technicianProfileId(),
          event.serviceRequestId()));
    } catch (RuntimeException e) {
      log.error("Could not notify the opening of maintenance window {}: {}",
          event.maintenanceWindowId(), e.getMessage(), e);
    }
  }

  /**
   * @param event the integration event published when a window closes
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(MaintenanceWindowClosedIntegrationEvent event) {
    try {
      notificationCommandService.handle(new NotifyMaintenanceWindowClosedCommand(
          event.maintenanceWindowId(),
          event.equipmentId(),
          event.origin(),
          event.endedAt(),
          event.closureReason(),
          event.technicianProfileId(),
          event.serviceRequestId()));
    } catch (RuntimeException e) {
      log.error("Could not notify the end of maintenance window {}: {}",
          event.maintenanceWindowId(), e.getMessage(), e);
    }
  }
}
