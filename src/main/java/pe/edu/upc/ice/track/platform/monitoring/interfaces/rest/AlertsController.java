package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertsQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertSeverityResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertStatusResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertTypeResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DismissAlertResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.PagedAlertResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST controller for alert listing, detail and lifecycle transitions.
 *
 * <p>Every endpoint is scoped to the authenticated caller. Reads are open to the owner of the unit
 * and to a technician actively assigned to it; lifecycle transitions are owner-only and record the
 * caller as their actor. An alert the caller may not reach answers 404, exactly like a missing
 * one; a transition its status forbids answers 409.</p>
 *
 * <p>The owner-wide listing is owner-only: it answers the alerts of every unit of the caller in one
 * paged read, active or closed, so no client has to ask unit by unit.</p>
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
   * List the alerts of every unit of the caller
   * @param status Restrict to these statuses, comma-separated or repeated; optional
   * @param equipmentId Restrict to a single unit of the caller, sent as {@code equipment_id}; optional
   * @param siteId Restrict to the units of a single site of the caller, sent as {@code site_id}; optional
   * @param type Restrict to these kinds of alert, comma-separated or repeated; optional
   * @param severity Restrict to these severities, comma-separated or repeated; optional
   * @param openedFrom Inclusive lower bound of the opening instant, sent as {@code opened_from}; optional
   * @param openedTo Inclusive upper bound of the opening instant, sent as {@code opened_to}; optional
   * @param page Zero-based page index; defaults to 0
   * @param size Page size, capped at 100; defaults to 20
   * @param authentication The authenticated principal
   * @return A {@link PagedAlertResource}, most recently opened first
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "List my alerts",
      description = "Lists the alerts of every unit of the authenticated owner in one read, whatever their "
          + "status, so both the active alerts and the alert history come from here. Optionally filtered by "
          + "status, unit, site, type, severity and the period the alert was opened in; every filter combines "
          + "with the others. Always paged, ordered by opened_at and then id, both descending. A size above "
          + "100 is capped to 100.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alerts found",
          content = @Content(schema = @Schema(implementation = PagedAlertResource.class))
      ),
      @ApiResponse(responseCode = "400",
          description = "VALIDATION_ERROR - unknown status, type or severity, opened_from after opened_to, "
              + "negative page or size below 1",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404",
          description = "SITE_NOT_FOUND or EQUIPMENT_NOT_FOUND - the site or unit of a filter is not reachable "
              + "by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getAlerts(
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these statuses, comma-separated", example = "OPEN,ACKNOWLEDGED")
      List<AlertStatusResource> status,
      @RequestParam(name = "equipment_id", required = false)
      @Parameter(description = "Restrict to a single unit of the caller", example = "10")
      Long equipmentId,
      @RequestParam(name = "site_id", required = false)
      @Parameter(description = "Restrict to the units of a single site of the caller", example = "1")
      Long siteId,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these kinds of alert, comma-separated", example = "TEMPERATURE_EXCURSION")
      List<AlertTypeResource> type,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these severities, comma-separated", example = "WARNING,CRITICAL")
      List<AlertSeverityResource> severity,
      @RequestParam(name = "opened_from", required = false)
      @Parameter(description = "Inclusive lower bound of opened_at, ISO 8601 with Z or an offset",
          example = "2026-11-01T00:00:00Z")
      Instant openedFrom,
      @RequestParam(name = "opened_to", required = false)
      @Parameter(description = "Inclusive upper bound of opened_at, ISO 8601 with Z or an offset",
          example = "2026-11-09T23:59:59Z")
      Instant openedTo,
      @RequestParam(defaultValue = "0")
      @Parameter(description = "Zero-based page index", example = "0")
      Integer page,
      @RequestParam(defaultValue = "20")
      @Parameter(description = "Page size, capped at 100", example = "20")
      Integer size,
      Authentication authentication) {
    var query = new GetAlertsQuery(
        authentication.getName(),
        toDomain(status, AlertStatusResource::toDomain),
        equipmentId,
        siteId,
        toDomain(type, AlertTypeResource::toDomain),
        toDomain(severity, AlertSeverityResource::toDomain),
        openedFrom,
        openedTo,
        page,
        size);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        alertQueryService.handle(query), AlertResourceFromEntityAssembler::toPagedResourceFromPage, HttpStatus.OK);
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
      @ApiResponse(responseCode = "404", description = "ALERT_NOT_FOUND - no such alert is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getAlertById(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    var query = new GetAlertByIdQuery(alertId, authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication));
    return alertQueryService.handle(query)
        .<ResponseEntity<?>>map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound("Alert", String.valueOf(alertId))));
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
   * @param authentication The authenticated principal, recorded as the acknowledging user
   * @return The updated {@link AlertResource}, now in ACKNOWLEDGED status
   */
  @PutMapping("/{alertId}/acknowledge")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Acknowledge an alert",
      description = "Transitions an OPEN alert of one of the caller's units to ACKNOWLEDGED, "
          + "signaling that the owner has seen it without yet resolving the underlying condition. "
          + "The caller and the instant are recorded as acknowledged_by and acknowledged_at.",
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
      @ApiResponse(
          responseCode = "404",
          description = "ALERT_NOT_FOUND - no such alert is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "INVALID_STATUS_TRANSITION - the alert is not OPEN",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      )
  })
  public ResponseEntity<?> acknowledgeAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    var result = alertCommandService.handle(new AcknowledgeAlertCommand(alertId, authentication.getName()));
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result, AlertResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
  }

  /**
   * Resolve an alert
   * @param alertId The alert unique identifier
   * @param authentication The authenticated principal, recorded as the resolving user
   * @return The updated {@link AlertResource}, now in RESOLVED status
   */
  @PutMapping("/{alertId}/resolve")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Resolve an alert",
      description = "Closes an OPEN or ACKNOWLEDGED alert of one of the caller's units once the underlying "
          + "condition has been corrected. The caller and the instant are recorded as resolved_by and "
          + "resolved_at, and total_duration_seconds becomes available.",
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
      @ApiResponse(
          responseCode = "404",
          description = "ALERT_NOT_FOUND - no such alert is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "INVALID_STATUS_TRANSITION - the alert is already RESOLVED or DISMISSED",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      )
  })
  public ResponseEntity<?> resolveAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      Authentication authentication) {
    var result = alertCommandService.handle(new ResolveAlertCommand(alertId, authentication.getName()));
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result, AlertResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
  }

  /**
   * Dismiss an alert
   * @param alertId The alert unique identifier
   * @param resource The justification for the dismissal
   * @param authentication The authenticated principal, recorded as the dismissing user
   * @return The updated {@link AlertResource}, now in DISMISSED status
   */
  @PutMapping(value = "/{alertId}/dismiss", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Dismiss an alert",
      description = "Closes an OPEN or ACKNOWLEDGED alert of one of the caller's units as a false alarm, such "
          + "as a planned defrost cycle, without asserting that the underlying condition was resolved. A "
          + "non-blank reason is mandatory; evidence is optional. The caller, the instant and the reason are "
          + "recorded as dismissed_by, dismissed_at and dismiss_reason. Dismissed alerts are excluded from "
          + "thermal excursion reports.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert dismissed",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "VALIDATION_ERROR - the body is missing, the reason is blank, or a field is too long",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(
          responseCode = "404",
          description = "ALERT_NOT_FOUND - no such alert is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "INVALID_STATUS_TRANSITION - the alert is already RESOLVED or DISMISSED",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      )
  })
  public ResponseEntity<?> dismissAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId,
      @Valid @RequestBody DismissAlertResource resource,
      Authentication authentication) {
    var command = new DismissAlertCommand(alertId, authentication.getName(), resource.reason(), resource.evidence());
    var result = alertCommandService.handle(command);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result, AlertResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
  }

  /**
   * Translates a multi-value filter into its domain counterpart. An empty entry, as left by a
   * trailing comma, is ignored rather than failing the whole request.
   */
  private static <R, D> Set<D> toDomain(List<R> values, Function<R, D> toDomain) {
    if (values == null) {
      return Set.of();
    }
    return values.stream()
        .filter(Objects::nonNull)
        .map(toDomain)
        .collect(Collectors.toUnmodifiableSet());
  }
}
