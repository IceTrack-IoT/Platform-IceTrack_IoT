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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.OwnerCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.OwnerQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileAccessEvaluator;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileView;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.OwnerProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateOwnerResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.OwnerProfileResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateOwnerCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;
import java.util.Optional;

/**
 * REST controller that exposes the owner profiles.
 *
 * <p>Profiles are exposed per role only: there is deliberately no polymorphic
 * {@code /api/v1/profiles} endpoint, so an owner payload never carries a technician attribute.</p>
 *
 * <p>There is deliberately no creation endpoint either: an owner profile is only ever created
 * together with its platform account, through the IAM owner registration flows and the profiles
 * ACL facade. An owner profile can only be updated by the account it belongs to, and the update
 * accepts the profile's {@code version} in {@code If-Match}.</p>
 *
 * <p>{@code GET /me} answers the caller's own profile without threading its account identifier.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles/owners", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Owner Profiles", description = "Ice track owner profile endpoints")
public class OwnerProfilesController {
  private static final String OWNER_RESOURCE = "Owner";

  private final OwnerQueryService ownerQueryService;
  private final OwnerCommandService ownerCommandService;
  private final ProfileAccessEvaluator profileAccessEvaluator;

  /**
   * Constructor
   * @param ownerQueryService      The {@link OwnerQueryService} instance
   * @param ownerCommandService    The {@link OwnerCommandService} instance
   * @param profileAccessEvaluator The {@link ProfileAccessEvaluator} deciding what a caller may read
   */
  public OwnerProfilesController(
      OwnerQueryService ownerQueryService,
      OwnerCommandService ownerCommandService,
      ProfileAccessEvaluator profileAccessEvaluator) {
    this.ownerQueryService = ownerQueryService;
    this.ownerCommandService = ownerCommandService;
    this.profileAccessEvaluator = profileAccessEvaluator;
  }

  /**
   * Get the caller's own owner profile, as a list
   *
   * <p>There is no tenant directory: the list holds at most the caller's own profile.
   * The list shape is kept so existing clients keep working.</p>
   *
   * @param authentication The authenticated principal
   * @return A list holding the caller's {@link OwnerProfileResource}, or empty
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
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
  public ResponseEntity<List<OwnerProfileResource>> getAllOwners(Authentication authentication) {
    var ownerResources = profileAccessEvaluator.resolveOwnerProfile(authentication).stream()
        .map(OwnerProfileResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(ownerResources);
  }

  /**
   * Get the caller's own owner profile
   *
   * @param authentication The authenticated principal
   * @return The caller's {@link OwnerProfileResource}, or 404 when the account holds no owner profile
   */
  @GetMapping("/me")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get my owner profile",
      description = "Retrieves the owner profile of the authenticated account, resolved from the access token; no "
          + "identifier is needed. The address comes both composed and in the parts an update takes.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Owner found",
          content = @Content(schema = @Schema(implementation = OwnerProfileResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "ACCESS_DENIED - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "OWNER_NOT_FOUND - the account holds no owner profile")
  })
  public ResponseEntity<?> getMyOwnerProfile(Authentication authentication) {
    return profileAccessEvaluator.resolveOwnerProfile(authentication)
        .<ResponseEntity<?>>map(owner -> ResponseEntity.ok(OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(owner)))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(OWNER_RESOURCE, "me")));
  }

  /**
   * Get an owner profile by ID
   * @param ownerId The owner profile ID
   * @param authentication The authenticated principal
   * @return An {@link OwnerProfileResource} resource for the owner, trimmed to what the caller may
   *         see, or 404 when the caller may not see it at all
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
      Long ownerId,
      Authentication authentication
  ) {
    return toVisibleResource(ownerQueryService.handle(new GetOwnerByIdQuery(ownerId)), authentication)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(OWNER_RESOURCE, ownerId.toString())));
  }

  /**
   * Get the owner profile bound to a platform account
   * @param userId The platform account ID
   * @param authentication The authenticated principal
   * @return An {@link OwnerProfileResource} resource for the owner, trimmed to what the caller may
   *         see, or 404 when the caller may not see it at all
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
      Long userId,
      Authentication authentication
  ) {
    return toVisibleResource(ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(userId))), authentication)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(OWNER_RESOURCE, "user " + userId)));
  }

  /**
   * Trims an owner profile to what the caller may see: all of it for the owner
   * itself, contact channels for a technician working on one of the owner's requests, nothing
   * otherwise.
   */
  private Optional<OwnerProfileResource> toVisibleResource(
      Optional<OwnerProfile> owner, Authentication authentication) {
    return owner.flatMap(profile -> {
      var view = profileAccessEvaluator.ownerProfileViewFor(profile, authentication);
      return view == ProfileView.NONE
          ? Optional.empty()
          : Optional.of(OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(profile, view));
    });
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
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "') and @profileAccessEvaluator.isOwnerSelf(#ownerId, authentication)")
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
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not this owner"),
      @ApiResponse(responseCode = "409", description = "CONCURRENT_MODIFICATION_CONFLICT - If-Match names a version "
          + "other than the current one, or another write overlapped this one")
  })
  public ResponseEntity<?> updateOwner(
      @PathVariable
      @Parameter(description = "Owner profile unique identifier", example = "1", required = true)
      Long ownerId,
      @Valid @RequestBody UpdateOwnerResource resource,
      @RequestHeader(value = IfMatchHeaderAssembler.IF_MATCH_HEADER, required = false)
      @Parameter(description = "Version of the profile being edited, as read from its version property", example = "\"2\"")
      String ifMatch
  ) {
    var updateOwnerCommand = UpdateOwnerCommandFromResourceAssembler.toCommandFromResource(ownerId, resource, ifMatch);
    var result = ownerCommandService.handle(updateOwnerCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        OwnerProfileResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
