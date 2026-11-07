package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

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
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertPolicyQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.UpdateAlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertPolicyResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller for reading and updating per-equipment alert policies.
 *
 * <p>The platform-wide default policy is seeded from configuration at startup and is not writable
 * here: a tenant must never be able to change the policy every other tenant is evaluated against.</p>
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
   * @param authentication The authenticated principal
   * @return An {@link AlertPolicyResource} resource, falling back to the platform-wide default
   *         policy when no equipment-specific policy is active
   */
  @GetMapping("/equipment/{equipmentId}")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "Get alert policy by equipment",
      description = "Retrieves the alert policy applicable to an equipment: the equipment-specific "
          + "policy when one is active, otherwise the platform-wide default. Owners read the policy "
          + "of their own units; technicians of units they hold an active service request on.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Policy found",
          content = @Content(schema = @Schema(implementation = AlertPolicyResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<AlertPolicyResource> getPolicyByEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "1", required = true)
      Long equipmentId,
      Authentication authentication) {
    var query = new GetAlertPolicyByEquipmentQuery(equipmentId, authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication));
    return alertPolicyQueryService.handle(query)
        .map(policy -> ResponseEntity.ok(AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(policy)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Create or update the alert policy of one of the caller's units
   * @param resource The {@link UpdateAlertPolicyResource} instance
   * @param authentication The authenticated principal
   * @return The created or updated {@link AlertPolicyResource}
   */
  @PutMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Create or update an alert policy",
      description = "Creates or updates the alert policy of one of the caller's units. The "
          + "platform-wide default policy is managed by configuration and cannot be changed here.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Policy created or updated successfully",
          content = @Content(schema = @Schema(implementation = AlertPolicyResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> updatePolicy(
      @Valid @RequestBody UpdateAlertPolicyResource resource,
      Authentication authentication) {
    var command = new UpdateAlertPolicyCommand(
        resource.equipmentId(), authentication.getName(), resource.sustainedExcursionMinutes(),
        resource.hysteresisMarginCelsius(), resource.missedSyncWindowsForOffline());
    return ResponseEntityAssembler.toResponseEntityFromResult(
        alertPolicyCommandService.handle(command),
        AlertPolicyResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
