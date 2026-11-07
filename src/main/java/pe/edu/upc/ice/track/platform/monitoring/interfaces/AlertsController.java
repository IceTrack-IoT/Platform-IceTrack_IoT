package pe.edu.upc.ice.track.platform.monitoring.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;

import java.util.List;

/**
 * REST controller for alert listing, detail and lifecycle transitions.
 *
 * <p>Every endpoint is scoped to the authenticated caller. Reads are open to the owner of the unit
 * and to a technician actively assigned to it; lifecycle transitions are owner-only. An alert the
 * caller may not reach answers 404, exactly like a missing one.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Alerts", description = "Alert listing, detail and lifecycle endpoints")
public class AlertsController {

  private final AlertCommandService alertCommandService;
  private final AlertQueryService alertQueryService;

  /**
   * Constructor
   * @param alertCommandService The {@link AlertCommandService} instance
   * @param alertQueryService The {@link AlertQueryService} instance
   */
  public AlertsController(AlertCommandService alertCommandService, AlertQueryService alertQueryService) {
    this.alertCommandService = alertCommandService;
    this.alertQueryService = alertQueryService;
  }

  /**
   * Get an alert by ID
   * @param alertId The alert unique identifier
   * @param authentication The authenticated principal
   * @return An {@link AlertResource} resource for the alert
   */
  @GetMapping("/{alertId}")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "Get alert by ID",
      description = "Retrieves a specific alert, including its type, severity, status and the "
          + "peak temperature observed during the excursion that raised it. Owners read alerts of "
          + "their own units; technicians read alerts of units they hold an active service request on.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert found",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "404", description = "No such alert is reachable by the caller")
  })
  public ResponseEntity<AlertResource> getAlertById(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    var query = new GetAlertByIdQuery(alertId, authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication));
    return alertQueryService.handle(query)
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Get the open alerts of an equipment
   * @param equipmentId The equipment unique identifier
   * @param authentication The authenticated principal
   * @return A list of {@link AlertResource} resources currently OPEN or ACKNOWLEDGED for the equipment
   */
  @GetMapping("/equipment/{equipmentId}/open")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "Get open alerts by equipment",
      description = "Retrieves the alerts currently OPEN or ACKNOWLEDGED for an equipment, "
          + "used to populate the equipment detail view and the dashboard open-alerts card. "
          + "A unit the caller may not read yields an empty list.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Open alerts found",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<List<AlertResource>> getOpenAlertsByEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "1", required = true)
      Long equipmentId,
      Authentication authentication) {
    var query = new GetOpenAlertsByEquipmentQuery(equipmentId, authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication));
    var alerts = alertQueryService.handle(query);
    return ResponseEntity.ok(alerts.stream().map(AlertResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  /**
   * Acknowledge an open alert
   * @param alertId The alert unique identifier
   * @param authentication The authenticated principal
   * @return The updated {@link AlertResource}, now in ACKNOWLEDGED status
   */
  @PutMapping("/{alertId}/acknowledge")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Acknowledge an alert",
      description = "Transitions an OPEN alert of one of the caller's units to ACKNOWLEDGED, "
          + "signaling that the owner has seen it without yet resolving the underlying condition.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert acknowledged",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such alert is reachable by the caller")
  })
  public ResponseEntity<AlertResource> acknowledgeAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    return alertCommandService.handle(new AcknowledgeAlertCommand(alertId, authentication.getName()))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Resolve an alert
   * @param alertId The alert unique identifier
   * @param authentication The authenticated principal
   * @return The updated {@link AlertResource}, now in RESOLVED status
   */
  @PutMapping("/{alertId}/resolve")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Resolve an alert",
      description = "Closes an alert of one of the caller's units once the underlying condition has "
          + "stopped applying, recording the moment of resolution.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert resolved",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such alert is reachable by the caller")
  })
  public ResponseEntity<AlertResource> resolveAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    return alertCommandService.handle(new ResolveAlertCommand(alertId, authentication.getName()))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Dismiss an alert
   * @param alertId The alert unique identifier
   * @param authentication The authenticated principal
   * @return The updated {@link AlertResource}, now in DISMISSED status
   */
  @PutMapping("/{alertId}/dismiss")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Dismiss an alert",
      description = "Closes an alert of one of the caller's units as not actionable, without "
          + "asserting that the underlying condition was resolved.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert dismissed",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such alert is reachable by the caller")
  })
  public ResponseEntity<AlertResource> dismissAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    return alertCommandService.handle(new DismissAlertCommand(alertId, authentication.getName()))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }
}
