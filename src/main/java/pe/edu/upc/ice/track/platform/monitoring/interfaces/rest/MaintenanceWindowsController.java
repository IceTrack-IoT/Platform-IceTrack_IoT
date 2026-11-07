package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.MaintenanceWindowQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetMaintenanceWindowsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.MaintenanceWindowResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.MaintenanceWindowResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.time.Instant;

/**
 * REST controller through which an owner audits the maintenance windows of its units.
 *
 * <p>Windows are opened and closed only by the units' gateways, on
 * {@code POST /api/v1/telemetry/events}; this controller only reads them. A unit the caller does not
 * own answers 404, exactly like a unit that does not exist.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/maintenance-windows", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Maintenance Windows", description = "Maintenance window audit endpoints")
public class MaintenanceWindowsController {

  private static final String EQUIPMENT_RESOURCE = "Equipment";

  private final MaintenanceWindowQueryService maintenanceWindowQueryService;

  public MaintenanceWindowsController(MaintenanceWindowQueryService maintenanceWindowQueryService) {
    this.maintenanceWindowQueryService = maintenanceWindowQueryService;
  }

  /**
   * Lists the maintenance windows of one of the caller's units.
   *
   * @param equipmentId    the unit, sent as the {@code equipment_id} query parameter
   * @param from           inclusive lower bound of the window start, optional
   * @param to             inclusive upper bound of the window start, optional
   * @param authentication the authenticated principal
   * @return the windows, newest first
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "List the maintenance windows of a unit",
      description = "Returns the maintenance windows of one of the caller's units, newest first: origin, start, "
          + "expiration, end, closure reason, the service request and technician when known, and an unlinked flag. "
          + "from and to, both optional and ISO-8601, bound the window start inclusively.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Windows found; empty when the unit has none",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceWindowResource.class)))),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR - a missing or malformed parameter, or "
          + "from after to",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "EQUIPMENT_NOT_FOUND - the unit does not exist or belongs "
          + "to another owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getMaintenanceWindows(
      @RequestParam(name = "equipment_id")
      @Parameter(description = "Unit whose windows are listed", example = "10", required = true)
      Long equipmentId,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      @Parameter(description = "Inclusive lower bound of the window start", example = "2026-11-01T00:00:00Z")
      Instant from,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      @Parameter(description = "Inclusive upper bound of the window start", example = "2026-11-30T23:59:59Z")
      Instant to,
      Authentication authentication) {
    final GetMaintenanceWindowsByEquipmentQuery query;
    try {
      query = new GetMaintenanceWindowsByEquipmentQuery(equipmentId, from, to, authentication.getName());
    } catch (IllegalArgumentException e) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.validationError("maintenance-window", e.getMessage()));
    }
    return maintenanceWindowQueryService.handle(query)
        .<ResponseEntity<?>>map(windows -> ResponseEntity.ok(windows.stream()
            .map(MaintenanceWindowResourceFromEntityAssembler::toResourceFromEntity)
            .toList()))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(equipmentId))));
  }
}
