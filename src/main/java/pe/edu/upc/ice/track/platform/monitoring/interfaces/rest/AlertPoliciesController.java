package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetApplicableAlertPoliciesQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ApplicablePolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.UpdateAlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertPolicyResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.ApplicablePolicyResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ConditionalResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;

/**
 * REST controller for reading and updating per-equipment alert policies.
 *
 * <p>The platform-wide default policy is seeded from configuration at startup and is not writable
 * here: a tenant must never be able to change the policy every other tenant is evaluated against.</p>
 *
 * <p>The batch read answers the policy applicable to many units of an owner at once, conditionally,
 * so an overview does not ask unit by unit.</p>
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
   * Get the alert policy applicable to several units of the caller at once
   * @param equipmentIds The units asked for, comma-separated as {@code equipment_id}; every unit of
   *                     the caller when omitted
   * @param ifNoneMatch The entity tag of the answer the caller already holds, optional
   * @param authentication The authenticated principal
   * @return The {@link ApplicablePolicyResource} array with its {@code ETag}, or 304 when the
   *         caller's copy is current
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get the alert policies applicable to my equipment",
      description = "Resolves, in one read, the alert policy applicable to each unit asked for: the unit's "
          + "own policy when it has one, otherwise the platform-wide default, with a source telling which. "
          + "Without equipment_id it covers every unit of the authenticated owner; with it, at most 100 "
          + "units. A unit that does not exist or belongs to another owner is left out of the answer rather "
          + "than refused. Entries are sorted by equipment_id. The ETag header identifies the answer; "
          + "sending it back in If-None-Match answers 304 with an empty body while nothing changed.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Policies resolved; the ETag header identifies the answer",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicablePolicyResource.class)))
      ),
      @ApiResponse(responseCode = "304", description = "Answer unchanged since the ETag in If-None-Match; empty body"),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR - a malformed identifier or more than 100 of them",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getApplicablePolicies(
      @RequestParam(name = "equipment_id", required = false)
      @Parameter(description = "Units asked for, comma-separated, at most 100; every unit of the caller when omitted",
          example = "10,11,12")
      List<Long> equipmentIds,
      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false)
      @Parameter(description = "ETag of the answer the caller already holds", example = "\"3f2a9c0d1b7e4a5f8c6d2e1b0a9f8e7d\"")
      String ifNoneMatch,
      Authentication authentication) {
    var query = new GetApplicableAlertPoliciesQuery(authentication.getName(), equipmentIds);
    return ConditionalResponseAssembler.toConditionalResponse(
        ApplicablePolicyResourceFromEntityAssembler.toResourcesFromEntities(alertPolicyQueryService.handle(query)),
        ifNoneMatch);
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
      @ApiResponse(responseCode = "404", description = "EQUIPMENT_NOT_FOUND - no such equipment is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getPolicyByEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "1", required = true)
      Long equipmentId,
      Authentication authentication) {
    var query = new GetAlertPolicyByEquipmentQuery(equipmentId, authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication));
    return alertPolicyQueryService.handle(query)
        .<ResponseEntity<?>>map(policy -> ResponseEntity.ok(AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(policy)))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound("Equipment", String.valueOf(equipmentId))));
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
