package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.SeedDefaultAlertPolicyCommand;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAlertPolicySeedingEventHandlerTest {

  @Mock
  private AlertPolicyCommandService alertPolicyCommandService;

  @InjectMocks
  private DefaultAlertPolicySeedingEventHandler handler;

  @Test
  @DisplayName("on(ApplicationReadyEvent) -> asks the command service to seed the default policy (AAA)")
  void seedsDefaultPolicyOnStartup() {
    // Arrange
    var event = mock(ApplicationReadyEvent.class);

    // Act
    handler.on(event);

    // Assert
    verify(alertPolicyCommandService, times(1)).handle(new SeedDefaultAlertPolicyCommand());
  }
}
