package pe.edu.upc.ice.track.platform.monitoring.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.util.List;

/**
 * REST controller for alert listing, detail and lifecycle transitions (US-28, US-29).
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
   * @return An {@link AlertResource} resource for the alert
   */
  @GetMapping("/{alertId}")
  @Operation(
      summary = "Get alert by ID",
      description = "Retrieves a specific alert, including its type, severity, status and the "
          + "peak temperature observed during the excursion that raised it."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert found",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Alert not found")
  })
  public ResponseEntity<AlertResource> getAlertById(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId) {
    return alertQueryService.handle(new GetAlertByIdQuery(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Get the open alerts of an equipment
   * @param equipmentId The equipment unique identifier
   * @return A list of {@link AlertResource} resources currently OPEN or ACKNOWLEDGED for the equipment
   */
  @GetMapping("/equipment/{equipmentId}/open")
  @Operation(
      summary = "Get open alerts by equipment",
      description = "Retrieves the alerts currently OPEN or ACKNOWLEDGED for an equipment, "
          + "used to populate the equipment detail view and the dashboard open-alerts card."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Open alerts found",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      )
  })
  public ResponseEntity<List<AlertResource>> getOpenAlertsByEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "1", required = true)
      Long equipmentId) {
    var alerts = alertQueryService.handle(new GetOpenAlertsByEquipmentQuery(equipmentId));
    return ResponseEntity.ok(alerts.stream().map(AlertResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  /**
   * Acknowledge an open alert
   * @param alertId The alert unique identifier
   * @return The updated {@link AlertResource}, now in ACKNOWLEDGED status
   */
  @PutMapping("/{alertId}/acknowledge")
  @Operation(
      summary = "Acknowledge an alert",
      description = "Transitions an OPEN alert to ACKNOWLEDGED, signalling that a user has "
          + "seen it without yet resolving the underlying condition."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert acknowledged",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Alert not found")
  })
  public ResponseEntity<AlertResource> acknowledgeAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId) {
    return alertCommandService.handle(new AcknowledgeAlertCommand(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Resolve an alert
   * @param alertId The alert unique identifier
   * @return The updated {@link AlertResource}, now in RESOLVED status
   */
  @PutMapping("/{alertId}/resolve")
  @Operation(
      summary = "Resolve an alert",
      description = "Closes an alert once the underlying condition has stopped applying, "
          + "recording the moment of resolution."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert resolved",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Alert not found")
  })
  public ResponseEntity<AlertResource> resolveAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId) {
    return alertCommandService.handle(new ResolveAlertCommand(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Dismiss an alert
   * @param alertId The alert unique identifier
   * @return The updated {@link AlertResource}, now in DISMISSED status
   */
  @PutMapping("/{alertId}/dismiss")
  @Operation(
      summary = "Dismiss an alert",
      description = "Closes an alert as not actionable, without asserting that the underlying "
          + "condition was resolved."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Alert dismissed",
          content = @Content(schema = @Schema(implementation = AlertResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Alert not found")
  })
  public ResponseEntity<AlertResource> dismissAlert(
      @PathVariable
      @Parameter(description = "Alert unique identifier", example = "1", required = true)
      Long alertId) {
    return alertCommandService.handle(new DismissAlertCommand(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }
}
