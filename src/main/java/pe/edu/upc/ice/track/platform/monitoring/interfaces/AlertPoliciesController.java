package pe.edu.upc.ice.track.platform.monitoring.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertPolicyQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.UpdateAlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertPolicyResourceFromEntityAssembler;

/**
 * REST controller for creating and updating alert policies, global or per-equipment.
 */
@RestController
@RequestMapping(value = "/api/v1/alert-policies", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Alert Policies", description = "Alert policy lookup and configuration endpoints")
public class AlertPoliciesController {

  private final AlertPolicyCommandService alertPolicyCommandService;
  private final AlertPolicyQueryService alertPolicyQueryService;

  /**
   * Constructor
   * @param alertPolicyCommandService The {@link AlertPolicyCommandService} instance
   * @param alertPolicyQueryService The {@link AlertPolicyQueryService} instance
   */
  public AlertPoliciesController(
      AlertPolicyCommandService alertPolicyCommandService,
      AlertPolicyQueryService alertPolicyQueryService) {
    this.alertPolicyCommandService = alertPolicyCommandService;
    this.alertPolicyQueryService = alertPolicyQueryService;
  }

  /**
   * Get the alert policy applicable to an equipment
   * @param equipmentId The equipment unique identifier
   * @return An {@link AlertPolicyResource} resource, falling back to the platform-wide default
   *         policy when no equipment-specific policy is active
   */
  @GetMapping("/equipment/{equipmentId}")
  @Operation(
      summary = "Get alert policy by equipment",
      description = "Retrieves the alert policy applicable to an equipment: the equipment-specific "
          + "policy when one is active, otherwise the platform-wide default."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Policy found",
          content = @Content(schema = @Schema(implementation = AlertPolicyResource.class))
      )
  })
  public ResponseEntity<AlertPolicyResource> getPolicyByEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "1", required = true)
      Long equipmentId) {
    var policy = alertPolicyQueryService.handle(new GetAlertPolicyByEquipmentQuery(equipmentId));
    return ResponseEntity.ok(AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(policy));
  }

  /**
   * Create or update an alert policy
   * @param resource The {@link UpdateAlertPolicyResource} instance
   * @return The created or updated {@link AlertPolicyResource}
   */
  @PutMapping
  @Operation(
      summary = "Create or update an alert policy",
      description = "Creates or updates the alert policy for the given equipment, or the "
          + "platform-wide default policy when equipmentId is omitted."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Policy created or updated successfully",
          content = @Content(schema = @Schema(implementation = AlertPolicyResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  public ResponseEntity<AlertPolicyResource> updatePolicy(
      @Valid @RequestBody UpdateAlertPolicyResource resource) {
    var command = new UpdateAlertPolicyCommand(
        resource.equipmentId(), resource.sustainedExcursionMinutes(),
        resource.hysteresisMarginCelsius(), resource.missedSyncWindowsForOffline());
    var policy = alertPolicyCommandService.handle(command);
    return ResponseEntity.ok(AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(policy));
  }
}
