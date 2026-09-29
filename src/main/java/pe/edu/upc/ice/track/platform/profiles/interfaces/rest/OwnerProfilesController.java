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
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.OwnerCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.OwnerQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllOwnersQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.OwnerProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateOwnerResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.OwnerProfileResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateOwnerCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;

/**
 * REST controller that exposes the owner profiles.
 *
 * <p>Profiles are exposed per role only: there is deliberately no polymorphic
 * {@code /api/v1/profiles} endpoint, so an owner payload never carries a technician attribute.</p>
 *
 * <p>There is deliberately no creation endpoint either: an owner profile is only ever created
 * together with its platform account, through the IAM owner registration flows and the profiles
 * ACL facade. An owner profile can only be updated by the account it belongs to.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles/owners", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Owner Profiles", description = "Ice track owner profile endpoints")
public class OwnerProfilesController {
  private static final String OWNER_RESOURCE = "Owner";

  private final OwnerQueryService ownerQueryService;
  private final OwnerCommandService ownerCommandService;

  /**
   * Constructor
   * @param ownerQueryService   The {@link OwnerQueryService} instance
   * @param ownerCommandService The {@link OwnerCommandService} instance
   */
  public OwnerProfilesController(OwnerQueryService ownerQueryService, OwnerCommandService ownerCommandService) {
    this.ownerQueryService = ownerQueryService;
    this.ownerCommandService = ownerCommandService;
  }

  /**
   * Get all owner profiles
   * @return A list of {@link OwnerProfileResource} resources
   */
  @GetMapping
  @Operation(
      summary = "Get all owners",
      description = "Retrieves every ice track owner profile.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Owners found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = OwnerProfileResource.class)))
      )
  })
  public ResponseEntity<List<OwnerProfileResource>> getAllOwners() {
    var owners = ownerQueryService.handle(new GetAllOwnersQuery());
    var ownerResources = owners.stream()
        .map(OwnerProfileResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(ownerResources);
  }

  /**
   * Get an owner profile by ID
   * @param ownerId The owner profile ID
   * @return An {@link OwnerProfileResource} resource for the owner
   */
  @GetMapping("/{ownerId}")
  @Operation(
      summary = "Get owner by ID",
      description = "Retrieves an ice track owner profile by its unique identifier.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Owner found",
          content = @Content(schema = @Schema(implementation = OwnerProfileResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Owner not found")
  })
  public ResponseEntity<?> getOwnerById(
      @PathVariable
      @Parameter(description = "Owner profile unique identifier", example = "1", required = true)
      Long ownerId
  ) {
    var owner = ownerQueryService.handle(new GetOwnerByIdQuery(ownerId));
    if (owner.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(OWNER_RESOURCE, ownerId.toString()));
    }
    return ResponseEntity.ok(OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(owner.get()));
  }

  /**
   * Get the owner profile bound to a platform account
   * @param userId The platform account ID
   * @return An {@link OwnerProfileResource} resource for the owner
   */
  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get owner by user ID",
      description = "Retrieves the ice track owner profile bound to a platform account.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Owner found",
          content = @Content(schema = @Schema(implementation = OwnerProfileResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "No owner is bound to the account")
  })
  public ResponseEntity<?> getOwnerByUserId(
      @PathVariable
      @Parameter(description = "Platform account unique identifier", example = "42", required = true)
      Long userId
  ) {
    var owner = ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(userId)));
    if (owner.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(OWNER_RESOURCE, "user " + userId));
    }
    return ResponseEntity.ok(OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(owner.get()));
  }

  /**
   * Update an owner profile
   *
   * <p>Only an account holding {@code OWNER_ROLE} may call this, and only for the owner profile
   * bound to its own account. Authorities are the raw role names, hence {@code hasAuthority}
   * rather than {@code hasRole}, which would look for a {@code ROLE_} prefix.</p>
   *
   * @param ownerId  The owner profile ID
   * @param resource The {@link UpdateOwnerResource} payload
   * @return An {@link OwnerProfileResource} resource for the updated owner
   */
  @PutMapping(value = "/{ownerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @profileAccessEvaluator.isOwnerSelf(#ownerId, authentication)")
  @Operation(
      summary = "Update an owner",
      description = "Replaces the name, contact details and RUC of the caller's own owner profile. "
          + "The email address belongs to the platform account and cannot be changed here.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Owner updated",
          content = @Content(schema = @Schema(implementation = OwnerProfileResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a RUC that is not 11 digits"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this owner")
  })
  public ResponseEntity<?> updateOwner(
      @PathVariable
      @Parameter(description = "Owner profile unique identifier", example = "1", required = true)
      Long ownerId,
      @Valid @RequestBody UpdateOwnerResource resource
  ) {
    var updateOwnerCommand = UpdateOwnerCommandFromResourceAssembler.toCommandFromResource(ownerId, resource);
    var result = ownerCommandService.handle(updateOwnerCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        OwnerProfileResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
