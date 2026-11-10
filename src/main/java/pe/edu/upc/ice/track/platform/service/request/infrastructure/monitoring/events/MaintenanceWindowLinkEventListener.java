package pe.edu.upc.ice.track.platform.service.request.infrastructure.monitoring.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowClosedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowOpenedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowPromotedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ServiceRequestCommandService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CompleteMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RecordMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;

import java.util.function.Supplier;

/**
 * Inbound infrastructure adapter that subscribes to the maintenance window integration events of
 * Monitoring and Alerting and keeps the window links of the service requests up to date.
 *
 * <p>This is the only class in {@code service.request} that knows a {@code monitoring} context exists,
 * and the single Anti-Corruption Layer point where the window origin - received as plain text - is
 * mapped onto this context's {@link MaintenanceOrigin}.</p>
 *
 * <p>Runs after the window's transaction committed, so a link that cannot be recorded never undoes
 * the window. Every failure is logged and swallowed: an event naming an order that was not in
 * progress is acknowledged without retry, as an inconsistency to look into rather than a fault.</p>
 */
@Component
@Slf4j
public class MaintenanceWindowLinkEventListener {

  private final ServiceRequestCommandService serviceRequestCommandService;

  public MaintenanceWindowLinkEventListener(ServiceRequestCommandService serviceRequestCommandService) {
    this.serviceRequestCommandService = serviceRequestCommandService;
  }

  /**
   * Links an opened window to its order, when it names one.
   *
   * @param event the integration event published by Monitoring and Alerting
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(MaintenanceWindowOpenedIntegrationEvent event) {
    if (event.serviceRequestId() == null) {
      return;
    }
    record(event.maintenanceWindowId(), () -> new RecordMaintenanceWindowLinkCommand(
        event.serviceRequestId(), event.maintenanceWindowId(), event.equipmentId(), toOrigin(event.origin()),
        event.technicianProfileId(), event.startedAt(), event.startedAt()));
  }

  /**
   * Links a window taken over from the app to its order, when it names one and was not linked yet.
   *
   * @param event the integration event published by Monitoring and Alerting
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(MaintenanceWindowPromotedIntegrationEvent event) {
    if (event.serviceRequestId() == null) {
      return;
    }
    record(event.maintenanceWindowId(), () -> new RecordMaintenanceWindowLinkCommand(
        event.serviceRequestId(), event.maintenanceWindowId(), event.equipmentId(), MaintenanceOrigin.APP,
        event.technicianProfileId(), event.startedAt(), event.promotedAt()));
  }

  /**
   * Completes the link of a closed window, when it was linked to an order.
   *
   * @param event the integration event published by Monitoring and Alerting
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(MaintenanceWindowClosedIntegrationEvent event) {
    if (event.serviceRequestId() == null) {
      return;
    }
    try {
      serviceRequestCommandService.handle(new CompleteMaintenanceWindowLinkCommand(
          event.serviceRequestId(), event.maintenanceWindowId(), event.endedAt(), event.closureReason()));
    } catch (RuntimeException e) {
      log.error("Could not complete the link of maintenance window {} on service request {}: {}",
          event.maintenanceWindowId(), event.serviceRequestId(), e.getMessage(), e);
    }
  }

  private void record(Long windowId, Supplier<RecordMaintenanceWindowLinkCommand> command) {
    try {
      serviceRequestCommandService.handle(command.get());
    } catch (RuntimeException e) {
      log.error("Could not link maintenance window {} to its service request: {}", windowId, e.getMessage(), e);
    }
  }

  private static MaintenanceOrigin toOrigin(String origin) {
    return switch (origin) {
      case "APP" -> MaintenanceOrigin.APP;
      case "BUTTON" -> MaintenanceOrigin.BUTTON;
      default -> throw new IllegalArgumentException("Unmapped maintenance window origin: " + origin);
    };
  }
}
