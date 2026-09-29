package pe.edu.upc.ice.track.platform.profiles.interfaces.rest;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.TechnicianCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TechnicianProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateTechnicianResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.TechnicianProfileResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateTechnicianCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;

/**
 * REST controller that exposes the technician profiles.
 *
 * <p>Profiles are exposed per role only: there is deliberately no polymorphic
 * {@code /api/v1/profiles} endpoint, so a technician payload never carries an owner attribute.</p>
 *
 * <p>There is deliberately no creation endpoint either: a technician profile is only ever created
 * together with its platform account, through the IAM technician registration flows and the
 * profiles ACL facade. A technician profile can only be updated by the account it belongs to.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles/technicians", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Technician Profiles", description = "Ice track maintenance technician profile endpoints")
public class TechnicianProfilesController {
  private static final String TECHNICIAN_RESOURCE = "Technician";

  private final TechnicianQueryService technicianQueryService;
  private final TechnicianCommandService technicianCommandService;

  /**
   * Constructor
   * @param technicianQueryService   The {@link TechnicianQueryService} instance
   * @param technicianCommandService The {@link TechnicianCommandService} instance
   */
  public TechnicianProfilesController(
      TechnicianQueryService technicianQueryService, TechnicianCommandService technicianCommandService) {
    this.technicianQueryService = technicianQueryService;
    this.technicianCommandService = technicianCommandService;
  }

  /**
   * Get all technician profiles
   * @return A list of {@link TechnicianProfileResource} resources
   */
  @GetMapping
  @Operation(
      summary = "Get all technicians",
      description = "Retrieves every ice track maintenance technician profile.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technicians found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = TechnicianProfileResource.class)))
      )
  })
  public ResponseEntity<List<TechnicianProfileResource>> getAllTechnicians() {
    var technicians = technicianQueryService.handle(new GetAllTechniciansQuery());
    var technicianResources = technicians.stream()
        .map(TechnicianProfileResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(technicianResources);
  }

  /**
   * Get a technician profile by ID
   * @param technicianId The technician profile ID
   * @return A {@link TechnicianProfileResource} resource for the technician
   */
  @GetMapping("/{technicianId}")
  @Operation(
      summary = "Get technician by ID",
      description = "Retrieves a maintenance technician profile by its unique identifier.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technician found",
          content = @Content(schema = @Schema(implementation = TechnicianProfileResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Technician not found")
  })
  public ResponseEntity<?> getTechnicianById(
      @PathVariable
      @Parameter(description = "Technician profile unique identifier", example = "1", required = true)
      Long technicianId
  ) {
    var technician = technicianQueryService.handle(new GetTechnicianByIdQuery(technicianId));
    if (technician.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(TECHNICIAN_RESOURCE, technicianId.toString()));
    }
    return ResponseEntity.ok(TechnicianProfileResourceFromEntityAssembler.toResourceFromEntity(technician.get()));
  }

  /**
   * Get the technician profile bound to a platform account
   * @param userId The platform account ID
   * @return A {@link TechnicianProfileResource} resource for the technician
   */
  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get technician by user ID",
      description = "Retrieves the maintenance technician profile bound to a platform account.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technician found",
          content = @Content(schema = @Schema(implementation = TechnicianProfileResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "No technician is bound to the account")
  })
  public ResponseEntity<?> getTechnicianByUserId(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "43", required = true)
      Long userId
  ) {
    var technician = technicianQueryService.handle(new GetTechnicianByUserIdQuery(new UserId(userId)));
    if (technician.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(TECHNICIAN_RESOURCE, "user " + userId));
    }
    return ResponseEntity.ok(TechnicianProfileResourceFromEntityAssembler.toResourceFromEntity(technician.get()));
  }

  /**
   * Update a technician profile
   *
   * <p>Only an account holding {@code TECHNICIAN_ROLE} may call this, and only for the technician
   * profile bound to its own account. Authorities are the raw role names, hence
   * {@code hasAuthority} rather than {@code hasRole}, which would look for a {@code ROLE_}
   * prefix.</p>
   *
   * @param technicianId The technician profile ID
   * @param resource     The {@link UpdateTechnicianResource} payload
   * @return A {@link TechnicianProfileResource} resource for the updated technician
   */
  @PutMapping(value = "/{technicianId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('TECHNICIAN_ROLE') and @profileAccessEvaluator.isTechnicianSelf(#technicianId, authentication)")
  @Operation(
      summary = "Update a technician",
      description = "Replaces the name, contact details, speciality and certification number of the caller's own "
          + "technician profile. The email address belongs to the platform account and cannot be changed here.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technician updated",
          content = @Content(schema = @Schema(implementation = TechnicianProfileResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a malformed certification number"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this technician")
  })
  public ResponseEntity<?> updateTechnician(
      @PathVariable
      @Parameter(description = "Technician profile unique identifier", example = "1", required = true)
      Long technicianId,
      @Valid @RequestBody UpdateTechnicianResource resource
  ) {
    var updateTechnicianCommand = UpdateTechnicianCommandFromResourceAssembler.toCommandFromResource(technicianId, resource);
    var result = technicianCommandService.handle(updateTechnicianCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        TechnicianProfileResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
