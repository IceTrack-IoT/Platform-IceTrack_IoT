package pe.edu.upc.ice.track.platform.profiles.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.DashboardConfigCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.DashboardConfigQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ResetDashboardConfigToDefaultCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ToggleCardVisibilityCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CreateDashboardConfigResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.DashboardConfigResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardDefaultsResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardLayoutResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.DashboardConfigResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.InitializeDashboardConfigCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateDashboardDefaultsCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateDashboardLayoutCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller that exposes the dashboard configurations.
 *
 * <p>A dashboard configuration is personal: every endpoint is scoped to a platform account and
 * may only be called by that account itself. Cards are sub-resources of the configuration - they
 * have no endpoint of their own outside it, and every change returns the whole, updated
 * configuration. The configuration is created with one card of each type, and cards are never
 * added or deleted afterwards: they are only shown, hidden and reordered.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles/dashboard-configs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dashboard Configs", description = "User dashboard personalization endpoints")
public class DashboardConfigsController {
  private static final String DASHBOARD_CONFIG_RESOURCE = "Dashboard_Config";

  private final DashboardConfigQueryService dashboardConfigQueryService;
  private final DashboardConfigCommandService dashboardConfigCommandService;

  /**
   * Constructor
   * @param dashboardConfigQueryService   The {@link DashboardConfigQueryService} instance
   * @param dashboardConfigCommandService The {@link DashboardConfigCommandService} instance
   */
  public DashboardConfigsController(
      DashboardConfigQueryService dashboardConfigQueryService,
      DashboardConfigCommandService dashboardConfigCommandService) {
    this.dashboardConfigQueryService = dashboardConfigQueryService;
    this.dashboardConfigCommandService = dashboardConfigCommandService;
  }

  /**
   * Get the dashboard configuration of a platform account
   * @param userId The platform account ID
   * @return A {@link DashboardConfigResource} resource for the configuration
   */
  @GetMapping("/user/{userId}")
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Get dashboard configuration by user ID",
      description = "Retrieves the dashboard configuration, cards included, of the caller's own account.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Dashboard configuration found",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration")
  })
  public ResponseEntity<?> getDashboardConfigByUserId(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId
  ) {
    var dashboardConfig = dashboardConfigQueryService.handle(new GetDashboardConfigByUserIdQuery(userId));
    if (dashboardConfig.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(DASHBOARD_CONFIG_RESOURCE, "user " + userId));
    }
    return ResponseEntity.ok(DashboardConfigResourceFromEntityAssembler.toResourceFromEntity(dashboardConfig.get()));
  }

  /**
   * Create the dashboard configuration of a platform account
   * @param resource The {@link CreateDashboardConfigResource} payload
   * @return A {@link DashboardConfigResource} resource for the created configuration
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#resource.userId(), authentication)")
  @Operation(
      summary = "Create a dashboard configuration",
      description = "Creates the dashboard configuration of the caller's own account in the default layout: one "
          + "visible card of each type, in the order MONITORED_EQUIPMENT, OPEN_ALERTS, ACTIVE_ORDERS, "
          + "EQUIPMENT_STATUS. An account has at most one configuration.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Dashboard configuration created",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "409", description = "The account already has a dashboard configuration")
  })
  public ResponseEntity<?> createDashboardConfig(@Valid @RequestBody CreateDashboardConfigResource resource) {
    var initializeDashboardConfigCommand =
        InitializeDashboardConfigCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = dashboardConfigCommandService.handle(initializeDashboardConfigCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        DashboardConfigResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Replace the card layout of the dashboard of a platform account
   * @param userId   The platform account ID
   * @param resource The {@link UpdateDashboardLayoutResource} payload
   * @return A {@link DashboardConfigResource} resource for the updated configuration
   */
  @PutMapping(value = "/user/{userId}/layout", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Update the dashboard layout",
      description = "Replaces the position and visibility of every card on the dashboard of the caller's own "
          + "account. The layout must place each card of the dashboard exactly once, at the positions 1 to N.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Dashboard layout updated",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid layout, such as a missing, unknown or repeated "
          + "card, or a repeated or out of range position"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration")
  })
  public ResponseEntity<?> updateLayout(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId,
      @Valid @RequestBody UpdateDashboardLayoutResource resource
  ) {
    var updateDashboardLayoutCommand = UpdateDashboardLayoutCommandFromResourceAssembler.toCommandFromResource(userId, resource);
    var result = dashboardConfigCommandService.handle(updateDashboardLayoutCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        DashboardConfigResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Show a hidden dashboard card, or hide a shown one
   * @param userId The platform account ID
   * @param cardId The card ID
   * @return A {@link DashboardConfigResource} resource for the updated configuration
   */
  @PatchMapping("/user/{userId}/cards/{cardId}/visibility")
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Toggle a dashboard card's visibility",
      description = "Shows the card when hidden, hides it when shown. A hidden card keeps its data and its position.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Card visibility toggled",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration, or the dashboard has no such card")
  })
  public ResponseEntity<?> toggleCardVisibility(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId,
      @PathVariable
      @Parameter(description = "Dashboard card unique identifier", example = "7", required = true)
      Long cardId
  ) {
    var result = dashboardConfigCommandService.handle(new ToggleCardVisibilityCommand(userId, cardId));
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        DashboardConfigResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Restore the default card layout of the dashboard of a platform account
   * @param userId The platform account ID
   * @return A {@link DashboardConfigResource} resource for the updated configuration
   */
  @PostMapping("/user/{userId}/reset-defaults")
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Reset the dashboard layout",
      description = "Restores the default card layout of the dashboard of the caller's own account: every card "
          + "visible, in the order MONITORED_EQUIPMENT, OPEN_ALERTS, ACTIVE_ORDERS, EQUIPMENT_STATUS. The default "
          + "site and temperature range are left unchanged.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Dashboard layout reset",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration")
  })
  public ResponseEntity<?> resetToDefaults(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId
  ) {
    var result = dashboardConfigCommandService.handle(new ResetDashboardConfigToDefaultCommand(userId));
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        DashboardConfigResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Replace the site and temperature range a dashboard opens on
   * @param userId   The platform account ID
   * @param resource The {@link UpdateDashboardDefaultsResource} payload
   * @return A {@link DashboardConfigResource} resource for the updated configuration
   */
  @PutMapping(value = "/user/{userId}/defaults", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Update dashboard defaults",
      description = "Replaces the site and temperature range the dashboard of the caller's own account opens on.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Dashboard defaults updated",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration")
  })
  public ResponseEntity<?> updateDefaults(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId,
      @Valid @RequestBody UpdateDashboardDefaultsResource resource
  ) {
    var updateDashboardDefaultsCommand =
        UpdateDashboardDefaultsCommandFromResourceAssembler.toCommandFromResource(userId, resource);
    var result = dashboardConfigCommandService.handle(updateDashboardDefaultsCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        DashboardConfigResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
