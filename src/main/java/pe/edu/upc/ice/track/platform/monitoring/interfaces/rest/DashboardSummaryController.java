package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.DashboardSummaryQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetDashboardSummaryQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DashboardSummaryResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.DashboardSummaryResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller for the owner dashboard summary.
 *
 * <p>A read model, not a resource of its own: it answers the few totals the overview cards of an
 * owner's dashboard show - units, active alerts, service requests, devices - so that no client has
 * to download every listing only to count its rows. Everything is scoped to the authenticated
 * caller; a site of another owner answers 404, exactly like a site that does not exist.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dashboard", description = "Owner dashboard summary endpoint")
public class DashboardSummaryController {

  private final DashboardSummaryQueryService dashboardSummaryQueryService;

  /**
   * Constructor
   * @param dashboardSummaryQueryService The {@link DashboardSummaryQueryService} instance
   */
  public DashboardSummaryController(DashboardSummaryQueryService dashboardSummaryQueryService) {
    this.dashboardSummaryQueryService = dashboardSummaryQueryService;
  }

  /**
   * Get the dashboard summary of the caller
   * @param siteId Restrict every figure to a single site of the caller, sent as {@code site_id}; optional
   * @param authentication The authenticated principal
   * @return A {@link DashboardSummaryResource}, or 404 when the site is not reachable by the caller
   */
  @GetMapping("/summary")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get my dashboard summary",
      description = "Returns, in one read, the totals of the authenticated owner's dashboard: sites, units per "
          + "status, units reporting, silent or never heard from, active alerts per status and severity, service "
          + "requests per status and linked devices. A unit is reporting while its last_reading_at lies within 60 "
          + "seconds times missed_sync_windows_for_offline of its applicable alert policy, judged by the server "
          + "clock, so clients need not compare instants themselves. site_id restricts every figure to one site.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Summary computed",
          content = @Content(schema = @Schema(implementation = DashboardSummaryResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR - site_id is not a number",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "404", description = "SITE_NOT_FOUND - no such site is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getDashboardSummary(
      @RequestParam(name = "site_id", required = false)
      @Parameter(description = "Restrict every figure to a single site of the caller", example = "1")
      Long siteId,
      Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        dashboardSummaryQueryService.handle(new GetDashboardSummaryQuery(authentication.getName(), siteId)),
        DashboardSummaryResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
