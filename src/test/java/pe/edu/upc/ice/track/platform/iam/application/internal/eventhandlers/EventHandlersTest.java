package pe.edu.upc.ice.track.platform.iam.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.RoleCommandService;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.PurgeExpiredRefreshTokensCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SeedRolesCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventHandlersTest {

  @Mock
  private RoleCommandService roleCommandService;
  @Mock
  private UserCommandService userCommandService;
  @Mock
  private ConfigurableApplicationContext applicationContext;

  private final SpringApplication springApplication = new SpringApplication();

  @Test
  @DisplayName("ApplicationReadyEventHandler.on(ApplicationReadyEvent) -> seeds the roles once (AAA)")
  void applicationReadySeedsRoles() {
    // Arrange
    when(applicationContext.getId()).thenReturn("icetrack");
    var handler = new ApplicationReadyEventHandler(roleCommandService);
    var event = new ApplicationReadyEvent(springApplication, new String[0], applicationContext, null);

    // Act
    handler.on(event);

    // Assert
    verify(roleCommandService, times(1)).handle(any(SeedRolesCommand.class));
  }

  @Test
  @DisplayName("ApplicationReadyEventHandler.on(ApplicationReadyEvent) -> propagates a seeding failure so startup fails fast (AAA)")
  void applicationReadyPropagatesFailure() {
    // Arrange
    when(applicationContext.getId()).thenReturn("icetrack");
    doThrow(new IllegalStateException("db down")).when(roleCommandService).handle(any(SeedRolesCommand.class));
    var handler = new ApplicationReadyEventHandler(roleCommandService);
    var event = new ApplicationReadyEvent(springApplication, new String[0], applicationContext, null);

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> handler.on(event));
  }



  @Test
  @DisplayName("RefreshTokenPurgeScheduler.purgeExpiredRefreshTokens() -> dispatches the purge command (AAA)")
  void schedulerDispatchesPurge() {
    // Arrange
    var scheduler = new RefreshTokenPurgeScheduler(userCommandService);

    // Act
    scheduler.purgeExpiredRefreshTokens();

    // Assert
    var captor = ArgumentCaptor.forClass(PurgeExpiredRefreshTokensCommand.class);
    verify(userCommandService, times(1)).handle(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new PurgeExpiredRefreshTokensCommand());
  }

  @Test
  @DisplayName("RefreshTokenPurgeScheduler.purgeExpiredRefreshTokens() -> swallows runtime failures so the schedule keeps running (AAA)")
  void schedulerSwallowsRuntimeFailures() {
    // Arrange
    doThrow(new IllegalStateException("db down")).when(userCommandService)
        .handle(any(PurgeExpiredRefreshTokensCommand.class));
    var scheduler = new RefreshTokenPurgeScheduler(userCommandService);

    // Act & Assert
    assertDoesNotThrow(scheduler::purgeExpiredRefreshTokens);
    verify(userCommandService, times(1)).handle(any(PurgeExpiredRefreshTokensCommand.class));
  }
}
