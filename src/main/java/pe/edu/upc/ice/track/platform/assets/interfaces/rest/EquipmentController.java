package pe.edu.upc.ice.track.platform.assets.interfaces.rest;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.authorization.OwnerIdentityResolver;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeStatusResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeThresholdResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.ChangeStatusCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.ChangeThresholdCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.RegisterEquipmentCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.UpdateEquipmentCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller exposing the refrigeration units of an owner (US-03, US-11, US-18).
 *
 * <p>Like the site endpoints, every action is scoped to the owner established by the API Gateway
 * and read through {@link OwnerIdentityResolver}; a unit of another owner is reported exactly like
 * a unit that does not exist. The temperature band and the status change have their own endpoints
 * rather than being folded into the update, so that each publishes its own domain event and each
 * can answer the status code its failure deserves - 400 for an inverted band, 409 for an illegal
 * transition.</p>
 *
 * <p>Id-scoped endpoints additionally carry a method-security check through
 * {@code @assetAccessEvaluator}, mirroring {@code @profileAccessEvaluator} in the profiles
 * context. Listing and registration resolve the caller owner via {@link OwnerIdentityResolver},
 * since there is no resource identifier to evaluate yet.</p>
 *
 * <p>The listing answers a plain array by default and a {@code PagedEquipmentResource} only when
 * the caller supplies both {@code page} and {@code size}, so pagination was added without
 * breaking the shape a non-paginating client already expects.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/equipments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Equipment", description = "Ice track equipment endpoints: registration, listing, threshold and status management")
public class EquipmentController {

  private static final String EQUIPMENT_RESOURCE = "Equipment";

  private final EquipmentQueryService equipmentQueryService;
  private final EquipmentCommandService equipmentCommandService;
  private final OwnerIdentityResolver ownerIdentityResolver;

  /**
   * Constructor
   *
   * @param equipmentQueryService The {@link EquipmentQueryService} instance
   * @param equipmentCommandService The {@link EquipmentCommandService} instance
   * @param ownerIdentityResolver The {@link OwnerIdentityResolver} instance
   */
  public EquipmentController(
      EquipmentQueryService equipmentQueryService,
      EquipmentCommandService equipmentCommandService,
      OwnerIdentityResolver ownerIdentityResolver) {
    this.equipmentQueryService = equipmentQueryService;
    this.equipmentCommandService = equipmentCommandService;
    this.ownerIdentityResolver = ownerIdentityResolver;
  }

  /**
   * Registers a refrigeration unit at one of the authenticated owner's sites.
   *
   * @param resource The {@link RegisterEquipmentResource} payload
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The registered {@link EquipmentResource}, with 201 Created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Register an equipment",
      description = "Registers a refrigeration unit at a site of the authenticated owner. The unit "
          + "starts AVAILABLE and disconnected, and its uid must be unique across the platform.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Equipment registered",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a threshold whose minimum is not below its maximum"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "The uid is already used by another unit")
  })
  public ResponseEntity<?> registerEquipment(
      @Valid @RequestBody RegisterEquipmentResource resource,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var command = RegisterEquipmentCommandFromResourceAssembler.toCommandFromResource(ownerId, resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        EquipmentResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Lists the authenticated owner's units, optionally filtered by status, type and site.
   *
   * @param status Restrict to a single operational status, optional
   * @param equipmentType Restrict to a single kind of unit, optional
   * @param siteId Restrict to a single site, optional
   * @param page Zero-based page index; supply together with {@code size} to page
   * @param size Page size; supply together with {@code page} to page
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return A plain {@link EquipmentResource} array, or a page wrapper when paging was requested
   */
  @GetMapping
  @Operation(
      summary = "List my equipment",
      description = "Lists the equipment of the authenticated owner showing at least name, type, uid "
          + "and status, optionally filtered by status, type and site (US-11, US-18). Supplying both "
          + "page and size returns a page wrapper with the total count instead of a plain array.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentResource.class)))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - no owner identity on the request")
  })
  public ResponseEntity<?> getMyEquipment(
      @RequestParam(required = false)
      @Parameter(description = "Restrict to a single status", example = "ON")
      StatusEquipmentResource status,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to a single equipment type", example = "FREEZER")
      EquipmentTypeResource equipmentType,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to a single site", example = "1")
      Long siteId,
      @RequestParam(required = false)
      @Parameter(description = "Zero-based page index; supply with size", example = "0")
      Integer page,
      @RequestParam(required = false)
      @Parameter(description = "Page size; supply with page", example = "20")
      Integer size,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var query = new GetEquipmentByOwnerQuery(
        ownerId,
        siteId,
        status == null ? null : status.toDomain(),
        equipmentType == null ? null : equipmentType.toDomain(),
        page,
        size);
    var result = equipmentQueryService.handle(query);
    if (query.isPaged()) {
      return ResponseEntity.ok(EquipmentResourceFromEntityAssembler.toPagedResourceFromPage(result));
    }
    return ResponseEntity.ok(result.content().stream()
        .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
        .toList());
  }

  /**
   * Fetches the detail of one of the authenticated owner's units.
   *
   * @param equipmentId The unit identifier
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The {@link EquipmentResource}, or 404 when it is not reachable by the caller
   */
  @GetMapping("/{equipmentId}")
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isEquipmentOwnedBy(#equipmentId, authentication)")
  @Operation(
      summary = "Get an equipment by ID",
      description = "Retrieves the full detail of one of the authenticated owner's equipment units (US-18).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment found",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<?> getEquipmentById(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var equipment = equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId, ownerId));
    if (equipment.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(equipmentId)));
    }
    return ResponseEntity.ok(EquipmentResourceFromEntityAssembler.toResourceFromEntity(equipment.get()));
  }

  /**
   * Replaces the descriptive data and maintenance interval of one of the owner's units.
   *
   * @param equipmentId The unit identifier
   * @param resource The {@link UpdateEquipmentResource} payload
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The updated {@link EquipmentResource}, or 404 when it is not reachable by the caller
   */
  @PutMapping(value = "/{equipmentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isEquipmentOwnedBy(#equipmentId, authentication)")
  @Operation(
      summary = "Update an equipment",
      description = "Replaces the name, type and preventive maintenance interval of one of the "
          + "authenticated owner's units (US-03). The uid, the site and the threshold are not "
          + "editable here.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment updated",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a non-positive maintenance interval"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<?> updateEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      @Valid @RequestBody UpdateEquipmentResource resource,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var command = UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource(equipmentId, ownerId, resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        EquipmentResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Replaces the acceptable temperature band of one of the owner's units.
   *
   * @param equipmentId The unit identifier
   * @param resource The {@link ChangeThresholdResource} payload
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The updated {@link EquipmentResource}, or the failure status of the change
   */
  @PutMapping(value = "/{equipmentId}/threshold", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isEquipmentOwnedBy(#equipmentId, authentication)")
  @Operation(
      summary = "Change the temperature threshold",
      description = "Replaces the acceptable temperature band of one of the authenticated owner's "
          + "units. A band whose minimum is not strictly below its maximum is rejected with 400 and "
          + "no domain event is emitted.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Threshold updated",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid band, such as a minimum greater than or equal to its maximum"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<?> changeThreshold(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      @Valid @RequestBody ChangeThresholdResource resource,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var command = ChangeThresholdCommandFromResourceAssembler.toCommandFromResource(
        equipmentId, ownerId, resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        EquipmentResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Moves one of the owner's units to a new operational status.
   *
   * @param equipmentId The unit identifier
   * @param resource The {@link ChangeStatusResource} payload
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The updated {@link EquipmentResource}, or 409 for a transition the matrix forbids
   */
  @PutMapping(value = "/{equipmentId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isEquipmentOwnedBy(#equipmentId, authentication)")
  @Operation(
      summary = "Change the equipment status",
      description = "Moves one of the authenticated owner's units to a new operational status. "
          + "Allowed transitions: AVAILABLE to ON or OFF, ON to OFF or OFFLINE, OFF to ON, AVAILABLE "
          + "or OFFLINE, OFFLINE to ON, OFF or AVAILABLE. A forbidden transition is rejected with 409.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Status changed",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "The requested status transition is not allowed")
  })
  public ResponseEntity<?> changeStatus(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      @Valid @RequestBody ChangeStatusResource resource,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var command = ChangeStatusCommandFromResourceAssembler.toCommandFromResource(equipmentId, ownerId, resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        EquipmentResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Builds the 403 returned when the authenticated account owns no owner profile, and therefore
   * owns no site and no equipment either.
   *
   * <p>Distinct from the 401 the security filter chain answers for a missing or invalid token:
   * there the caller is unknown, here the caller is known and simply has nothing to act on.</p>
   *
   * @return the 403 response
   */
  private static ResponseEntity<?> noOwnerProfile() {
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no site or equipment"));
  }
}
