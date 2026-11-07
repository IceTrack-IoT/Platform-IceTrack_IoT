package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.SeedDefaultAlertPolicyCommand;

/**
 * Seeds the platform-wide default alert policy once the application is ready.
 *
 * <p>Without a default, evaluating a reading for a unit that has no policy of its own had nothing
 * to fall back on, and ingestion failed with a 500 after the reading row was saved. Mirrors how
 * IAM seeds its roles.</p>
 */
@Service
@Slf4j
public class DefaultAlertPolicySeedingEventHandler {

  private final AlertPolicyCommandService alertPolicyCommandService;

  public DefaultAlertPolicySeedingEventHandler(AlertPolicyCommandService alertPolicyCommandService) {
    this.alertPolicyCommandService = alertPolicyCommandService;
  }

  /**
   * @param event Spring Boot readiness event
   */
  @EventListener
  public void on(ApplicationReadyEvent event) {
    log.info("Verifying the platform-wide default alert policy");
    alertPolicyCommandService.handle(new SeedDefaultAlertPolicyCommand());
  }
}
