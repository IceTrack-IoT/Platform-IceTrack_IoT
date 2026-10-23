package pe.edu.upc.ice.track.platform.service.request.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.service.request.application.queryservices.ServiceRequestQueryService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListEquipmentInterventionsQuery;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.InterventionResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.InterventionResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

/**
 * REST controller exposing the intervention history of a unit.
 *
 * <p>Mounted under the equipment path of {@code assets}, since it is a view of a unit, but owned by
 * this context, which is the one that records interventions. {@code assets} is only consulted, through
 * the ACL, to check the owner of the unit.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/assets/equipments/{equipmentId}/interventions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Equipment Interventions", description = "Intervention history of a refrigeration unit")
public class EquipmentInterventionsController {

  private static final String EQUIPMENT_RESOURCE = "Equipment";

  private final ServiceRequestQueryService serviceRequestQueryService;

  /**
   * Constructor
   *
   * @param serviceRequestQueryService The {@link ServiceRequestQueryService} instance
   */
  public EquipmentInterventionsController(ServiceRequestQueryService serviceRequestQueryService) {
    this.serviceRequestQueryService = serviceRequestQueryService;
  }

  /**
   * Lists the past interventions performed on a unit, most recent first.
   *
   * @param equipmentId    The unit identifier
   * @param authentication The authenticated principal, whose name and role identify the caller
   * @return A plain {@link InterventionResource} array, or 404 when the unit is not reachable by the
   *         caller
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('OWNER_ROLE', 'TECHNICIAN_ROLE')")
  @Operation(
      summary = "List the interventions of an equipment",
      description = "Lists the interventions that completed service requests on a unit, most recent "
          + "first. Open to the owner of the unit and to a technician assigned to an active or completed "
          + "request on it.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Interventions found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = InterventionResource.class)))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<?> getEquipmentInterventions(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      Authentication authentication) {
    var completedRequests = serviceRequestQueryService.handle(new ListEquipmentInterventionsQuery(
        equipmentId,
        authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication)));
    if (completedRequests.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(equipmentId)));
    }
    return ResponseEntity.ok(completedRequests.get().stream()
        .map(InterventionResourceFromEntityAssembler::toResourceFromEntity)
        .toList());
  }
}
