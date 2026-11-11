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
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentConnectivityQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentDirectoryQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeStatusResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeThresholdResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentDirectoryEntryResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateEquipmentResource;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.ChangeStatusCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.ChangeThresholdCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.EquipmentDirectoryEntryResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.RegisterEquipmentCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.UpdateEquipmentCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ConditionalResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing the refrigeration units of an owner.
 *
 * <p>Like the site endpoints, every action is scoped to the authenticated caller: the controller
 * only hands the principal's username to the command and query services, which resolve it to an
 * owner and apply the ownership rules. A unit of another owner is reported exactly like a unit that
 * does not exist. The temperature band and the status change have their own endpoints
 * rather than being folded into the update, so that each publishes its own domain event and each
 * can answer the status code its failure deserves - 400 for an inverted band, 409 for an illegal
 * transition.</p>
 *
 * <p>Every endpoint declares its roles through method security, so an account outside them is
 * answered with 403 before a service is reached. Registration, the listing, the update and the
 * threshold change are {@code OWNER_ROLE} only. Fetching a unit and changing its status are also
 * open to {@code TECHNICIAN_ROLE}: the role the caller acts in is handed to the services, which do
 * not scope a technician by ownership, since a technician services units of any owner.</p>
 *
 * <p>The listing answers a plain array by default and a {@code PagedEquipmentResource} only when
 * the caller supplies both {@code page} and {@code size}, so pagination was added without
 * breaking the shape a non-paginating client already expects.</p>
 *
 * <p>The directory is the lean, conditional counterpart of the listing: only the identity of every
 * unit, with an {@code ETag}, for the screens that need to name units rather than monitor them.
 * Every {@link EquipmentResource} carries the name of the unit's site, so neither read needs the
 * site list to show where a unit is installed.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/assets/equipments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Equipment", description = "Ice track equipment endpoints: registration, listing, threshold and status management")
public class EquipmentController {

  private static final String EQUIPMENT_RESOURCE = "Equipment";

  private final EquipmentQueryService equipmentQueryService;
  private final EquipmentCommandService equipmentCommandService;

  /**
   * Constructor
   *
   * @param equipmentQueryService The {@link EquipmentQueryService} instance
   * @param equipmentCommandService The {@link EquipmentCommandService} instance
   */
  public EquipmentController(
      EquipmentQueryService equipmentQueryService,
      EquipmentCommandService equipmentCommandService) {
    this.equipmentQueryService = equipmentQueryService;
    this.equipmentCommandService = equipmentCommandService;
  }

  /**
   * Registers a refrigeration unit at one of the authenticated owner's sites.
   *
   * @param resource The {@link RegisterEquipmentResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The registered {@link EquipmentResource}, with 201 Created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Register an equipment",
      description = "Registers a refrigeration unit at a site of the authenticated owner. The unit "
          + "starts ACTIVE and disconnected, and its uid must be unique across the platform.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Equipment registered",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a threshold outside 0 to 30 °C or whose minimum is not below its maximum"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "The uid is already used by another unit")
  })
  public ResponseEntity<?> registerEquipment(
      @Valid @RequestBody RegisterEquipmentResource resource,
      Authentication authentication) {
    var command = RegisterEquipmentCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        this::toResource,
        HttpStatus.CREATED);
  }

  /**
   * Lists the authenticated owner's units, optionally searched by name or uid and filtered by
   * status, type and site.
   *
   * @param search Case-insensitive text matched against the unit name or uid, optional
   * @param status Restrict to a single lifecycle status, optional
   * @param equipmentType Restrict to a single kind of unit, sent as {@code equipment_type}, optional
   * @param siteId Restrict to a single site, sent as {@code site_id}, optional
   * @param page Zero-based page index; supply together with {@code size} to page
   * @param size Page size, from 1 to 100; supply together with {@code page} to page
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return A plain {@link EquipmentResource} array, or a page wrapper when paging was requested
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "List my equipment",
      description = "Lists the equipment of the authenticated owner showing at least name, type, uid "
          + "and status, optionally searched by name or uid and filtered by status, type and site. "
          + "Every filter combines with the others and only ever narrows the caller's own units. "
          + "Supplying both page and size returns a page wrapper with the total count instead of "
          + "a plain array.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentResource.class)))),
      @ApiResponse(responseCode = "400", description = "Invalid filter or paging value, such as an unknown status or a page size above 100"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - no owner identity on the request"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner")
  })
  public ResponseEntity<?> getMyEquipment(
      @RequestParam(required = false)
      @Parameter(description = "Case-insensitive search matching equipment name or UID", example = "ESP32")
      String search,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to a single status", example = "ACTIVE")
      StatusEquipmentResource status,
      @RequestParam(name = "equipment_type", required = false)
      @Parameter(description = "Restrict to a single equipment type", example = "REFRIGERATOR")
      EquipmentTypeResource equipmentType,
      @RequestParam(name = "site_id", required = false)
      @Parameter(description = "Restrict to a single site", example = "1")
      Long siteId,
      @RequestParam(required = false)
      @Parameter(description = "Zero-based page index; supply with size", example = "0")
      Integer page,
      @RequestParam(required = false)
      @Parameter(description = "Page size, from 1 to 100; supply with page", example = "20")
      Integer size,
      Authentication authentication) {
    var query = new GetEquipmentByOwnerQuery(
        authentication.getName(),
        search,
        siteId,
        status == null ? null : status.toDomain(),
        equipmentType == null ? null : equipmentType.toDomain(),
        page,
        size);
    var result = equipmentQueryService.handle(query);
    var connectivity = equipmentQueryService.handle(new GetEquipmentConnectivityQuery(result.content()));
    if (query.isPaged()) {
      return ResponseEntity.ok(EquipmentResourceFromEntityAssembler.toPagedResourceFromPage(result, connectivity));
    }
    return ResponseEntity.ok(result.content().stream()
        .map(unit -> EquipmentResourceFromEntityAssembler.toResourceFromEntity(
            unit, connectivity.get(unit.getEquipmentId())))
        .toList());
  }

  /**
   * Answers the authenticated owner's equipment directory: the identity of every unit, with the name
   * of its site, in one conditional read.
   *
   * @param siteId         Restrict to a single site of the caller, sent as {@code site_id}, optional
   * @param status         Restrict to these statuses, comma-separated or repeated; optional
   * @param ifNoneMatch    The entity tag of the directory the caller already holds, optional
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The {@link EquipmentDirectoryEntryResource} array with its {@code ETag}, 304 when the
   *         caller's copy is current, or 404 when the site is not reachable by the caller
   */
  @GetMapping("/directory")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get my equipment directory",
      description = "Returns the identity of every unit of the authenticated owner - id, uid, name, type, "
          + "status and site, with the site name - sorted by name, unpaged. It carries no telemetry, "
          + "threshold or configuration version, so it only changes when a unit or a site is created, "
          + "renamed or changes status. The ETag header identifies the answer; sending it back in "
          + "If-None-Match answers 304 with an empty body while nothing changed.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Directory found; the ETag header identifies it",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentDirectoryEntryResource.class)))),
      @ApiResponse(responseCode = "304", description = "Directory unchanged since the ETag in If-None-Match; empty body"),
      @ApiResponse(responseCode = "400", description = "Invalid filter value, such as an unknown status"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner"),
      @ApiResponse(responseCode = "404", description = "SITE_NOT_FOUND - no such site is reachable by the caller")
  })
  public ResponseEntity<?> getMyEquipmentDirectory(
      @RequestParam(name = "site_id", required = false)
      @Parameter(description = "Restrict to a single site of the caller", example = "1")
      Long siteId,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these statuses, comma-separated", example = "ACTIVE,MAINTENANCE")
      List<StatusEquipmentResource> status,
      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false)
      @Parameter(description = "ETag of the directory the caller already holds", example = "\"3f2a9c0d1b7e4a5f8c6d2e1b0a9f8e7d\"")
      String ifNoneMatch,
      Authentication authentication) {
    var query = new GetEquipmentDirectoryQuery(authentication.getName(), siteId, toDomainStatuses(status));
    return ConditionalResponseAssembler.toConditionalResponseFromResult(
        equipmentQueryService.handle(query),
        EquipmentDirectoryEntryResourceFromEntityAssembler::toResourcesFromEntities,
        ifNoneMatch);
  }

  /**
   * Fetches the detail of one of the authenticated owner's units.
   *
   * @param equipmentId The unit identifier
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The {@link EquipmentResource}, or 404 when it is not reachable by the caller
   */
  @GetMapping("/{equipmentId}")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "Get an equipment by ID",
      description = "Retrieves the full detail of an equipment unit. Owners can only read their "
          + "own units; technicians can only read units they hold an active service request on "
          + "(assigned, accepted or in progress). Any other unit answers 404.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment found",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<?> getEquipmentById(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      Authentication authentication) {
    var equipment = equipmentQueryService.handle(new GetEquipmentByIdQuery(
        equipmentId,
        authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication)));
    if (equipment.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(equipmentId)));
    }
    return ResponseEntity.ok(toResource(equipment.get()));
  }

  /**
   * Replaces the descriptive data and maintenance interval of one of the owner's units.
   *
   * @param equipmentId The unit identifier
   * @param resource The {@link UpdateEquipmentResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The updated {@link EquipmentResource}, or 404 when it is not reachable by the caller
   */
  @PutMapping(value = "/{equipmentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Update an equipment",
      description = "Replaces the name, type and preventive maintenance interval of one of the "
          + "authenticated owner's units. The uid, the site and the threshold are not "
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
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "CONCURRENT_MODIFICATION_CONFLICT - If-Match names a version other than the current one")
  })
  public ResponseEntity<?> updateEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      @Valid @RequestBody UpdateEquipmentResource resource,
      @RequestHeader(value = IfMatchHeaderAssembler.IF_MATCH_HEADER, required = false)
      @Parameter(description = "Version of the unit being edited, as read from its version property", example = "\"2\"")
      String ifMatch,
      Authentication authentication) {
    var command = UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource(
        equipmentId, authentication.getName(), resource, ifMatch);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        this::toResource,
        HttpStatus.OK);
  }

  /**
   * Replaces the acceptable temperature band of one of the owner's units.
   *
   * @param equipmentId The unit identifier
   * @param resource The {@link ChangeThresholdResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The updated {@link EquipmentResource}, or the failure status of the change
   */
  @PutMapping(value = "/{equipmentId}/threshold", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Change the temperature threshold",
      description = "Replaces the acceptable temperature band of one of the authenticated owner's "
          + "units. Both bounds must lie within 0 to 30 °C and the minimum must be strictly below "
          + "the maximum; otherwise the request is rejected with 400, the previous band stays "
          + "active and no domain event is emitted. An accepted band increments config_version, "
          + "returned in the response, so the unit's edge gateway downloads it on its next poll.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Threshold updated",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid band: a bound outside 0 to 30 °C, or a minimum greater than or equal to its maximum"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "CONCURRENT_MODIFICATION_CONFLICT - If-Match names a version other than the current one")
  })
  public ResponseEntity<?> changeThreshold(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      @Valid @RequestBody ChangeThresholdResource resource,
      @RequestHeader(value = IfMatchHeaderAssembler.IF_MATCH_HEADER, required = false)
      @Parameter(description = "Version of the unit being edited, as read from its version property", example = "\"2\"")
      String ifMatch,
      Authentication authentication) {
    var command = ChangeThresholdCommandFromResourceAssembler.toCommandFromResource(
        equipmentId, authentication.getName(), resource, ifMatch);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        this::toResource,
        HttpStatus.OK);
  }

  /**
   * Moves one of the owner's units to a new operational status.
   *
   * @param equipmentId The unit identifier
   * @param resource The {@link ChangeStatusResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The updated {@link EquipmentResource}, or 409 for a transition the matrix forbids
   */
  @PutMapping(value = "/{equipmentId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Change the equipment status",
      description = "Moves one of the caller's own equipment units to a new lifecycle status. "
          + "Owner only: technicians cannot change a unit's status directly. "
          + "Allowed transitions: ACTIVE to MAINTENANCE, REPAIR or DEACTIVATED; MAINTENANCE to "
          + "ACTIVE, REPAIR or DEACTIVATED; REPAIR to ACTIVE or DEACTIVATED; DEACTIVATED to ACTIVE. "
          + "Maintenance is only entered from ACTIVE, and a deactivated unit must be reactivated "
          + "first. Re-sending the current status is a no-op. A forbidden transition is rejected "
          + "with 409.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Status changed",
          content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner, or is an owner without an owner profile"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "The requested status transition is not allowed, or "
          + "CONCURRENT_MODIFICATION_CONFLICT when If-Match names a version other than the current one")
  })
  public ResponseEntity<?> changeStatus(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "10", required = true)
      Long equipmentId,
      @Valid @RequestBody ChangeStatusResource resource,
      @RequestHeader(value = IfMatchHeaderAssembler.IF_MATCH_HEADER, required = false)
      @Parameter(description = "Version of the unit being edited, as read from its version property", example = "\"2\"")
      String ifMatch,
      Authentication authentication) {
    var command = ChangeStatusCommandFromResourceAssembler.toCommandFromResource(
        equipmentId, authentication.getName(), resource, ifMatch);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        equipmentCommandService.handle(command),
        this::toResource,
        HttpStatus.OK);
  }

  /**
   * Converts one unit into its resource, with its connectivity evaluated now.
   *
   * @param equipment the unit, already scoped to the caller
   * @return the resource
   */
  private EquipmentResource toResource(Equipment equipment) {
    var connectivity = equipmentQueryService.handle(new GetEquipmentConnectivityQuery(List.of(equipment)));
    return EquipmentResourceFromEntityAssembler.toResourceFromEntity(
        equipment, connectivity.get(equipment.getEquipmentId()));
  }

  /**
   * Translates the status filter into its domain counterpart. An empty entry, as left by a trailing
   * comma, is ignored rather than failing the whole request.
   */
  private static Set<StatusEquipment> toDomainStatuses(List<StatusEquipmentResource> statuses) {
    if (statuses == null) {
      return Set.of();
    }
    return statuses.stream()
        .filter(Objects::nonNull)
        .map(StatusEquipmentResource::toDomain)
        .collect(Collectors.toUnmodifiableSet());
  }
}
