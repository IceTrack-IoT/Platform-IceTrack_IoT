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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.RemoveCardFromDashboardCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ToggleCardVisibilityCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.AddCardResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CreateDashboardConfigResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.DashboardConfigResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardDefaultsResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.AddCardToDashboardCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.DashboardConfigResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.InitializeDashboardConfigCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateDashboardDefaultsCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller that exposes the dashboard configurations.
 *
 * <p>A dashboard configuration is personal: every endpoint is scoped to a platform account and
 * may only be called by that account itself. Cards are sub-resources of the configuration - they
 * have no endpoint of their own outside it, and every change returns the whole, updated
 * configuration.</p>
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
      description = "Creates the dashboard configuration of the caller's own account, with no cards. "
          + "An account has at most one configuration.",
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
   * Add a card to the dashboard of a platform account
   * @param userId   The platform account ID
   * @param resource The {@link AddCardResource} payload
   * @return A {@link DashboardConfigResource} resource for the updated configuration
   */
  @PostMapping(value = "/user/{userId}/cards", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Add a dashboard card",
      description = "Adds a card to the dashboard of the caller's own account. A dashboard shows each card type at most once.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Card added",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as an unknown card type or a negative order"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration"),
      @ApiResponse(responseCode = "409", description = "The dashboard already shows a card of that type")
  })
  public ResponseEntity<?> addCard(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId,
      @Valid @RequestBody AddCardResource resource
  ) {
    var addCardToDashboardCommand = AddCardToDashboardCommandFromResourceAssembler.toCommandFromResource(userId, resource);
    var result = dashboardConfigCommandService.handle(addCardToDashboardCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        DashboardConfigResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
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
      description = "Shows the card when hidden, hides it when shown.",
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
   * Remove a card from the dashboard of a platform account
   * @param userId The platform account ID
   * @param cardId The card ID
   * @return A {@link DashboardConfigResource} resource for the updated configuration
   */
  @DeleteMapping("/user/{userId}/cards/{cardId}")
  @PreAuthorize("@profileAccessEvaluator.isAccountSelf(#userId, authentication)")
  @Operation(
      summary = "Remove a dashboard card",
      description = "Removes a card from the dashboard of the caller's own account; the card is deleted.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Card removed",
          content = @Content(schema = @Schema(implementation = DashboardConfigResource.class))
      ),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this account"),
      @ApiResponse(responseCode = "404", description = "The account has no dashboard configuration, or the dashboard has no such card")
  })
  public ResponseEntity<?> removeCard(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId,
      @PathVariable
      @Parameter(description = "Dashboard card unique identifier", example = "7", required = true)
      Long cardId
  ) {
    var result = dashboardConfigCommandService.handle(new RemoveCardFromDashboardCommand(userId, cardId));
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
