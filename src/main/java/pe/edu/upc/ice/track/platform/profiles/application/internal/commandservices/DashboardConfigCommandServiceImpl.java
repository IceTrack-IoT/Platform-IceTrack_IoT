package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.DashboardConfigCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.InitializeDashboardConfigCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ResetDashboardConfigToDefaultCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ToggleCardVisibilityCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardDefaultsCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardLayoutCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardLayoutItem;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Dashboard Config Command Service Implementation
 *
 * <p>Every command first checks, through the {@link ExternalIamService} ACL, that the platform
 * account it targets exists: a dashboard configuration is never created for, nor changed on behalf
 * of, an account IAM does not know.</p>
 */
@Service
@Slf4j
public class DashboardConfigCommandServiceImpl implements DashboardConfigCommandService {

  private static final String USER_RESOURCE = "User";
  private static final String DASHBOARD_CONFIG_RESOURCE = "Dashboard_Config";
  private static final String DASHBOARD_CARD_RESOURCE = "Dashboard_Card";

  private final DashboardConfigRepository dashboardConfigRepository;
  private final ExternalIamService externalIamService;

  /**
   * Constructor
   *
   * @param dashboardConfigRepository The {@link DashboardConfigRepository} instance
   * @param externalIamService        The {@link ExternalIamService} instance
   */
  public DashboardConfigCommandServiceImpl(
      DashboardConfigRepository dashboardConfigRepository, ExternalIamService externalIamService) {
    this.dashboardConfigRepository = dashboardConfigRepository;
    this.externalIamService = externalIamService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<DashboardConfig, ApplicationError> handle(InitializeDashboardConfigCommand command) {
    if (!externalIamService.existsUserById(command.userId())) {
      return Result.failure(ApplicationError.notFound(USER_RESOURCE, command.userId().toString()));
    }
    try {
      var userId = new UserId(command.userId());
      if (dashboardConfigRepository.existsByUserId(userId)) {
        return Result.failure(ApplicationError.conflict(
            DASHBOARD_CONFIG_RESOURCE,
            "User '%s' already has a dashboard configuration".formatted(command.userId())));
      }
      var dashboardConfig = new DashboardConfig(
          userId,
          new SiteId(command.siteId()),
          new TemperatureRange(command.tempMin(), command.tempMax(), command.tempUnit(), command.tempLabel()));
      var savedDashboardConfig = dashboardConfigRepository.save(dashboardConfig);
      log.info("Created dashboard configuration {} for user {}",
          savedDashboardConfig.getDashboardConfigId(), command.userId());
      return Result.success(savedDashboardConfig);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(DASHBOARD_CONFIG_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Dashboard configuration creation", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<DashboardConfig, ApplicationError> handle(UpdateDashboardLayoutCommand command) {
    if (!externalIamService.existsUserById(command.userId())) {
      return Result.failure(ApplicationError.notFound(USER_RESOURCE, command.userId().toString()));
    }
    var existingDashboardConfig = dashboardConfigRepository.findByUserId(new UserId(command.userId()));
    if (existingDashboardConfig.isEmpty()) {
      return Result.failure(ApplicationError.notFound(DASHBOARD_CONFIG_RESOURCE, "user " + command.userId()));
    }
    try {
      var dashboardConfig = existingDashboardConfig.get();
      dashboardConfig.updateLayout(command.cards().stream()
          .map(item -> new CardLayoutItem(item.cardId(), item.order(), item.isVisible()))
          .toList());
      var savedDashboardConfig = dashboardConfigRepository.save(dashboardConfig);
      log.info("Updated the card layout of dashboard configuration {}", savedDashboardConfig.getDashboardConfigId());
      return Result.success(savedDashboardConfig);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(DASHBOARD_CARD_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Dashboard layout update", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<DashboardConfig, ApplicationError> handle(ToggleCardVisibilityCommand command) {
    if (!externalIamService.existsUserById(command.userId())) {
      return Result.failure(ApplicationError.notFound(USER_RESOURCE, command.userId().toString()));
    }
    var existingDashboardConfig = dashboardConfigRepository.findByUserId(new UserId(command.userId()));
    if (existingDashboardConfig.isEmpty()) {
      return Result.failure(ApplicationError.notFound(DASHBOARD_CONFIG_RESOURCE, "user " + command.userId()));
    }
    var dashboardConfig = existingDashboardConfig.get();
    if (dashboardConfig.findCard(command.cardId()).isEmpty()) {
      return Result.failure(ApplicationError.notFound(DASHBOARD_CARD_RESOURCE, command.cardId().toString()));
    }
    try {
      dashboardConfig.toggleCardVisibility(command.cardId());
      var savedDashboardConfig = dashboardConfigRepository.save(dashboardConfig);
      log.info("Toggled the visibility of card {} of dashboard configuration {}",
          command.cardId(), savedDashboardConfig.getDashboardConfigId());
      return Result.success(savedDashboardConfig);
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Dashboard card visibility toggle", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<DashboardConfig, ApplicationError> handle(ResetDashboardConfigToDefaultCommand command) {
    if (!externalIamService.existsUserById(command.userId())) {
      return Result.failure(ApplicationError.notFound(USER_RESOURCE, command.userId().toString()));
    }
    var existingDashboardConfig = dashboardConfigRepository.findByUserId(new UserId(command.userId()));
    if (existingDashboardConfig.isEmpty()) {
      return Result.failure(ApplicationError.notFound(DASHBOARD_CONFIG_RESOURCE, "user " + command.userId()));
    }
    try {
      var dashboardConfig = existingDashboardConfig.get();
      dashboardConfig.resetToDefaults();
      var savedDashboardConfig = dashboardConfigRepository.save(dashboardConfig);
      log.info("Reset dashboard configuration {} to the default card layout", savedDashboardConfig.getDashboardConfigId());
      return Result.success(savedDashboardConfig);
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Dashboard layout reset", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<DashboardConfig, ApplicationError> handle(UpdateDashboardDefaultsCommand command) {
    if (!externalIamService.existsUserById(command.userId())) {
      return Result.failure(ApplicationError.notFound(USER_RESOURCE, command.userId().toString()));
    }
    var existingDashboardConfig = dashboardConfigRepository.findByUserId(new UserId(command.userId()));
    if (existingDashboardConfig.isEmpty()) {
      return Result.failure(ApplicationError.notFound(DASHBOARD_CONFIG_RESOURCE, "user " + command.userId()));
    }
    try {
      var dashboardConfig = existingDashboardConfig.get();
      dashboardConfig.updateDefaults(
          new SiteId(command.siteId()),
          new TemperatureRange(command.tempMin(), command.tempMax(), command.tempUnit(), command.tempLabel()));
      var savedDashboardConfig = dashboardConfigRepository.save(dashboardConfig);
      log.info("Updated the defaults of dashboard configuration {}", savedDashboardConfig.getDashboardConfigId());
      return Result.success(savedDashboardConfig);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(DASHBOARD_CONFIG_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Dashboard defaults update", e.getMessage()));
    }
  }
}
