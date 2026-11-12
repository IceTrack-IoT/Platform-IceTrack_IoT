package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.InitializeDashboardConfigCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardDefaultsCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.initializeDashboardCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.resetCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.reverseLayoutCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.toggleCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.updateDefaultsCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.DASHBOARD_VERSION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.SITE_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedDashboard;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class DashboardConfigCommandServiceImplTest {

  private static final UserId USER = new UserId(USER_ID);

  @Mock
  private DashboardConfigRepository dashboardConfigRepository;
  @Mock
  private ExternalIamService externalIamService;

  @InjectMocks
  private DashboardConfigCommandServiceImpl service;

  private void givenUserExists() {
    when(externalIamService.existsUserById(USER_ID)).thenReturn(true);
  }

  private DashboardConfig givenStoredDashboard() {
    givenUserExists();
    var dashboard = persistedDashboard();
    when(dashboardConfigRepository.findByUserId(USER)).thenReturn(Optional.of(dashboard));
    return dashboard;
  }

  @Nested
  class Initialize {

    @Test
    @DisplayName("handle(InitializeDashboardConfigCommand) unknown user -> USER_NOT_FOUND, nothing else touched (AAA)")
    void unknownUser() {
      // Arrange
      when(externalIamService.existsUserById(USER_ID)).thenReturn(false);

      // Act
      var error = failureError(service.handle(initializeDashboardCommand()));

      // Assert
      assertThat(error.code()).isEqualTo("USER_NOT_FOUND");
      verifyNoInteractions(dashboardConfigRepository);
    }

    @Test
    @DisplayName("handle(InitializeDashboardConfigCommand) user already configured -> DASHBOARD_CONFIG_CONFLICT (AAA)")
    void alreadyConfigured() {
      // Arrange
      givenUserExists();
      when(dashboardConfigRepository.existsByUserId(USER)).thenReturn(true);

      // Act
      var error = failureError(service.handle(initializeDashboardCommand()));

      // Assert
      assertThat(error.code()).isEqualTo("DASHBOARD_CONFIG_CONFLICT");
      assertThat(error.details()).isEqualTo("User '9' already has a dashboard configuration");
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(InitializeDashboardConfigCommand) -> saves a dashboard with defaults and the four cards (AAA)")
    void succeeds() {
      // Arrange
      givenUserExists();
      when(dashboardConfigRepository.existsByUserId(USER)).thenReturn(false);
      when(dashboardConfigRepository.save(any(DashboardConfig.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      var saved = successValue(service.handle(initializeDashboardCommand()));

      // Assert
      assertThat(saved.getUserId()).isEqualTo(USER);
      assertThat(saved.getDefaultSiteId().siteId()).isEqualTo(SITE_ID);
      assertThat(saved.getDefaultTemperatureRange().label()).isEqualTo("Cold chain");
      assertThat(saved.getCards()).hasSize(CardType.values().length);
    }

    @Test
    @DisplayName("handle(InitializeDashboardConfigCommand) invalid temperature range -> VALIDATION_ERROR, nothing saved (AAA)")
    void invalidRange() {
      // Arrange
      givenUserExists();
      when(dashboardConfigRepository.existsByUserId(USER)).thenReturn(false);
      var command = new InitializeDashboardConfigCommand(USER_ID, SITE_ID, 9, 8, "C", "x");

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("Temperature range min must not be greater than max");
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(InitializeDashboardConfigCommand) missing site -> VALIDATION_ERROR (AAA)")
    void missingSite() {
      // Arrange
      givenUserExists();
      when(dashboardConfigRepository.existsByUserId(USER)).thenReturn(false);

      // Act
      var error = failureError(service.handle(new InitializeDashboardConfigCommand(USER_ID, null, 2, 8, "C", "x")));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("handle(InitializeDashboardConfigCommand) optimistic lock -> rethrown; other failure -> UNEXPECTED_ERROR (AAA)")
    void failures() {
      // Arrange
      givenUserExists();
      when(dashboardConfigRepository.existsByUserId(USER)).thenReturn(false);
      when(dashboardConfigRepository.save(any(DashboardConfig.class)))
          .thenThrow(new OptimisticLockingFailureException("stale"))
          .thenThrow(new IllegalStateException("boom"));

      // Act & Assert
      assertThatThrownBy(() -> service.handle(initializeDashboardCommand()))
          .isInstanceOf(OptimisticLockingFailureException.class);
      var error = failureError(service.handle(initializeDashboardCommand()));
      assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
      assertThat(error.message()).isEqualTo("Unexpected error in Dashboard configuration creation");
    }
  }

  @Nested
  class Preconditions {

    @Test
    @DisplayName("handle(any update command) unknown user -> USER_NOT_FOUND (AAA)")
    void unknownUser() {
      // Arrange
      when(externalIamService.existsUserById(USER_ID)).thenReturn(false);

      // Act & Assert
      assertThat(failureError(service.handle(reverseLayoutCommand(null))).code()).isEqualTo("USER_NOT_FOUND");
      assertThat(failureError(service.handle(toggleCommand(101L, null))).code()).isEqualTo("USER_NOT_FOUND");
      assertThat(failureError(service.handle(resetCommand())).code()).isEqualTo("USER_NOT_FOUND");
      assertThat(failureError(service.handle(updateDefaultsCommand(null))).code()).isEqualTo("USER_NOT_FOUND");
      verifyNoInteractions(dashboardConfigRepository);
    }

    @Test
    @DisplayName("handle(any update command) user without dashboard -> DASHBOARD_CONFIG_NOT_FOUND (AAA)")
    void missingDashboard() {
      // Arrange
      givenUserExists();
      when(dashboardConfigRepository.findByUserId(USER)).thenReturn(Optional.empty());

      // Act & Assert
      assertThat(failureError(service.handle(reverseLayoutCommand(null))).code()).isEqualTo("DASHBOARD_CONFIG_NOT_FOUND");
      assertThat(failureError(service.handle(toggleCommand(101L, null))).code()).isEqualTo("DASHBOARD_CONFIG_NOT_FOUND");
      assertThat(failureError(service.handle(resetCommand())).code()).isEqualTo("DASHBOARD_CONFIG_NOT_FOUND");
      assertThat(failureError(service.handle(updateDefaultsCommand(null))).code()).isEqualTo("DASHBOARD_CONFIG_NOT_FOUND");
      verify(dashboardConfigRepository, never()).save(any());
    }
  }

  @Nested
  class UpdateLayout {

    @Test
    @DisplayName("handle(UpdateDashboardLayoutCommand) matching version -> applies the layout and saves (AAA)")
    void succeeds() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard)).thenReturn(dashboard);

      // Act
      var saved = successValue(service.handle(reverseLayoutCommand(DASHBOARD_VERSION)));

      // Assert
      assertThat(saved.getCards()).extracting(DashboardCard::getCardId).containsExactly(104L, 103L, 102L, 101L);
      assertThat(saved.findCard(101L).orElseThrow().isVisible()).isFalse();
      verify(dashboardConfigRepository, times(1)).save(dashboard);
    }

    @Test
    @DisplayName("handle(UpdateDashboardLayoutCommand) stale version -> CONCURRENT_MODIFICATION_CONFLICT, unchanged (AAA)")
    void staleVersion() {
      // Arrange
      var dashboard = givenStoredDashboard();

      // Act
      var error = failureError(service.handle(reverseLayoutCommand(1L)));

      // Assert
      assertThat(error.code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
      assertThat(dashboard.getCards()).extracting(DashboardCard::getCardId).containsExactly(101L, 102L, 103L, 104L);
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(UpdateDashboardLayoutCommand) layout that does not fit -> VALIDATION_ERROR on Dashboard_Card (AAA)")
    void invalidLayout() {
      // Arrange
      givenStoredDashboard();
      var command = new pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardLayoutCommand(
          USER_ID, java.util.List.of());

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.message()).isEqualTo("Validation failed: Dashboard_Card");
      assertThat(error.details()).isEqualTo("Layout must place exactly 4 cards, 0 were given");
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(UpdateDashboardLayoutCommand) optimistic lock -> rethrown; other failure -> UNEXPECTED_ERROR (AAA)")
    void failures() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard))
          .thenThrow(new OptimisticLockingFailureException("stale"))
          .thenThrow(new IllegalStateException("boom"));

      // Act & Assert
      assertThatThrownBy(() -> service.handle(reverseLayoutCommand(null)))
          .isInstanceOf(OptimisticLockingFailureException.class);
      assertThat(failureError(service.handle(reverseLayoutCommand(null))).code()).isEqualTo("UNEXPECTED_ERROR");
    }
  }

  @Nested
  class ToggleVisibility {

    @Test
    @DisplayName("handle(ToggleCardVisibilityCommand) -> flips the card and saves (AAA)")
    void succeeds() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard)).thenReturn(dashboard);

      // Act
      var saved = successValue(service.handle(toggleCommand(102L, DASHBOARD_VERSION)));

      // Assert
      assertThat(saved.findCard(102L).orElseThrow().isVisible()).isFalse();
      assertThat(saved.findCard(101L).orElseThrow().isVisible()).isTrue();
    }

    @Test
    @DisplayName("handle(ToggleCardVisibilityCommand) unknown card -> DASHBOARD_CARD_NOT_FOUND (AAA)")
    void unknownCard() {
      // Arrange
      givenStoredDashboard();

      // Act
      var error = failureError(service.handle(toggleCommand(999L, null)));

      // Assert
      assertThat(error.code()).isEqualTo("DASHBOARD_CARD_NOT_FOUND");
      assertThat(error.message()).isEqualTo("Dashboard_Card not found: 999");
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(ToggleCardVisibilityCommand) stale version -> CONCURRENT_MODIFICATION_CONFLICT (AAA)")
    void staleVersion() {
      // Arrange
      var dashboard = givenStoredDashboard();

      // Act
      var error = failureError(service.handle(toggleCommand(102L, 99L)));

      // Assert
      assertThat(error.code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
      assertThat(dashboard.findCard(102L).orElseThrow().isVisible()).isTrue();
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(ToggleCardVisibilityCommand) optimistic lock -> rethrown; other failure -> UNEXPECTED_ERROR (AAA)")
    void failures() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard))
          .thenThrow(new OptimisticLockingFailureException("stale"))
          .thenThrow(new IllegalStateException("boom"));

      // Act & Assert
      assertThatThrownBy(() -> service.handle(toggleCommand(101L, null)))
          .isInstanceOf(OptimisticLockingFailureException.class);
      assertThat(failureError(service.handle(toggleCommand(101L, null))).message())
          .isEqualTo("Unexpected error in Dashboard card visibility toggle");
    }
  }

  @Nested
  class Reset {

    @Test
    @DisplayName("handle(ResetDashboardConfigToDefaultCommand) -> default order, all visible, saved (AAA)")
    void succeeds() {
      // Arrange
      var dashboard = givenStoredDashboard();
      dashboard.toggleCardVisibility(101L);
      when(dashboardConfigRepository.save(dashboard)).thenReturn(dashboard);

      // Act
      var saved = successValue(service.handle(resetCommand()));

      // Assert
      assertThat(saved.getCards()).allMatch(DashboardCard::isVisible);
      assertThat(saved.getCards()).extracting(DashboardCard::getCardType).containsExactly(CardType.values());
      verify(dashboardConfigRepository, times(1)).save(dashboard);
    }

    @Test
    @DisplayName("handle(ResetDashboardConfigToDefaultCommand) optimistic lock -> rethrown; other failure -> UNEXPECTED_ERROR (AAA)")
    void failures() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard))
          .thenThrow(new OptimisticLockingFailureException("stale"))
          .thenThrow(new IllegalStateException("boom"));

      // Act & Assert
      assertThatThrownBy(() -> service.handle(resetCommand())).isInstanceOf(OptimisticLockingFailureException.class);
      assertThat(failureError(service.handle(resetCommand())).message())
          .isEqualTo("Unexpected error in Dashboard layout reset");
    }
  }

  @Nested
  class UpdateDefaults {

    @Test
    @DisplayName("handle(UpdateDashboardDefaultsCommand) -> replaces site and range (unit normalized) and saves (AAA)")
    void succeeds() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard)).thenReturn(dashboard);

      // Act
      var saved = successValue(service.handle(updateDefaultsCommand(DASHBOARD_VERSION)));

      // Assert
      assertThat(saved.getDefaultSiteId().siteId()).isEqualTo(4L);
      assertThat(saved.getDefaultTemperatureRange().min()).isEqualTo(-20);
      assertThat(saved.getDefaultTemperatureRange().unit()).isEqualTo("C");
      assertThat(saved.getDefaultTemperatureRange().label()).isEqualTo("Freezer");
    }

    @Test
    @DisplayName("handle(UpdateDashboardDefaultsCommand) stale version -> CONCURRENT_MODIFICATION_CONFLICT (AAA)")
    void staleVersion() {
      // Arrange
      givenStoredDashboard();

      // Act
      var error = failureError(service.handle(updateDefaultsCommand(1L)));

      // Assert
      assertThat(error.code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(UpdateDashboardDefaultsCommand) invalid range -> VALIDATION_ERROR on Dashboard_Config (AAA)")
    void invalidRange() {
      // Arrange
      var dashboard = givenStoredDashboard();
      var command = new UpdateDashboardDefaultsCommand(USER_ID, 4L, 5, 1, "C", "x", null);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.message()).isEqualTo("Validation failed: Dashboard_Config");
      assertThat(dashboard.getDefaultSiteId().siteId()).isEqualTo(SITE_ID);
      verify(dashboardConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(UpdateDashboardDefaultsCommand) optimistic lock -> rethrown; other failure -> UNEXPECTED_ERROR (AAA)")
    void failures() {
      // Arrange
      var dashboard = givenStoredDashboard();
      when(dashboardConfigRepository.save(dashboard))
          .thenThrow(new OptimisticLockingFailureException("stale"))
          .thenThrow(new IllegalStateException("boom"));

      // Act & Assert
      assertThatThrownBy(() -> service.handle(updateDefaultsCommand(null)))
          .isInstanceOf(OptimisticLockingFailureException.class);
      assertThat(failureError(service.handle(updateDefaultsCommand(null))).message())
          .isEqualTo("Unexpected error in Dashboard defaults update");
    }
  }
}
