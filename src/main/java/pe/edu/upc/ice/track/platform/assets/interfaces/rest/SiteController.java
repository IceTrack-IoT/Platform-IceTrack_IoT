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
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.SiteQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentBySiteQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSitesByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.authorization.OwnerIdentityResolver;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterSiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.SiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.RegisterSiteCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.SiteResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.UpdateSiteInfoCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller exposing the sites of an owner (US-20).
 *
 * <p>Every endpoint acts on the owner established by the API Gateway, read through
 * {@link OwnerIdentityResolver}: no request body carries an owner and no endpoint accepts one as a
 * parameter, so a client cannot register a site for somebody else or read one that is not its
 * own. A request whose identity cannot be resolved is answered with 403, since the caller is
 * known and simply has nothing to act on.</p>
 *
 * <p>Id-scoped endpoints additionally carry a method-security check through
 * {@code @assetAccessEvaluator}, mirroring {@code @profileAccessEvaluator} in the profiles
 * context.</p>
 *
 * <p>This controller holds no business logic: it converts a payload into a command through its
 * atomic assembler, delegates to a command or query service, and converts the outcome into a
 * resource. The single decision it does make is which HTTP status a result maps to,
 * and that mapping is shared across the platform by
 * {@link pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/sites", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sites", description = "Ice track site endpoints: registration and listing of the premises where equipment is installed")
public class SiteController {

  private static final String SITE_RESOURCE = "Site";

  private final SiteQueryService siteQueryService;
  private final SiteCommandService siteCommandService;
  private final EquipmentQueryService equipmentQueryService;
  private final OwnerIdentityResolver ownerIdentityResolver;

  /**
   * Constructor
   *
   * @param siteQueryService The {@link SiteQueryService} instance
   * @param siteCommandService The {@link SiteCommandService} instance
   * @param equipmentQueryService The {@link EquipmentQueryService} instance, used to list the
   *                              equipment installed at a site
   * @param ownerIdentityResolver The {@link OwnerIdentityResolver} instance
   */
  public SiteController(
      SiteQueryService siteQueryService,
      SiteCommandService siteCommandService,
      EquipmentQueryService equipmentQueryService,
      OwnerIdentityResolver ownerIdentityResolver) {
    this.siteQueryService = siteQueryService;
    this.siteCommandService = siteCommandService;
    this.equipmentQueryService = equipmentQueryService;
    this.ownerIdentityResolver = ownerIdentityResolver;
  }

  /**
   * Registers a new site for the authenticated owner.
   *
   * @param resource The {@link RegisterSiteResource} payload
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The registered {@link SiteResource}, with 201 Created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Register a site",
      description = "Registers a new site for the authenticated owner. The owner is taken from the "
          + "authenticated identity and cannot be supplied in the payload.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Site registered",
          content = @Content(schema = @Schema(implementation = SiteResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as an address longer than 50 characters"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile")
  })
  public ResponseEntity<?> registerSite(
      @Valid @RequestBody RegisterSiteResource resource,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var command = RegisterSiteCommandFromResourceAssembler.toCommandFromResource(ownerId, resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        siteCommandService.handle(command),
        SiteResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Lists every site belonging to the authenticated owner.
   *
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The {@link SiteResource} list, with 200 OK
   */
  @GetMapping
  @Operation(
      summary = "List my sites",
      description = "Lists every site belonging to the authenticated owner, with its name, address, "
          + "contact name and contact phone number.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Sites found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = SiteResource.class)))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile")
  })
  public ResponseEntity<?> getMySites(Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var sites = siteQueryService.handle(new GetSitesByOwnerQuery(ownerId));
    return ResponseEntity.ok(sites.stream().map(SiteResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  /**
   * Fetches one of the authenticated owner's sites.
   *
   * <p>A site that does not exist and a site owned by somebody else both answer 404, so this
   * endpoint cannot be used to discover which site identifiers are real.</p>
   *
   * @param siteId The site identifier
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The {@link SiteResource}, or 404 when it is not reachable by the caller
   */
  @GetMapping("/{siteId}")
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isSiteOwnedBy(#siteId, authentication)")
  @Operation(
      summary = "Get a site by ID",
      description = "Retrieves one of the authenticated owner's sites by its unique identifier.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Site found",
          content = @Content(schema = @Schema(implementation = SiteResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller")
  })
  public ResponseEntity<?> getSiteById(
      @PathVariable
      @Parameter(description = "Site unique identifier", example = "1", required = true)
      Long siteId,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var site = siteQueryService.handle(new GetSiteByIdQuery(siteId, ownerId));
    if (site.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(SITE_RESOURCE, String.valueOf(siteId)));
    }
    return ResponseEntity.ok(SiteResourceFromEntityAssembler.toResourceFromEntity(site.get()));
  }

  /**
   * Lists the units installed at one of the authenticated owner's sites.
   *
   * @param siteId The site identifier
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The {@link EquipmentResource} list, or 404 when the site is not reachable by the caller
   */
  @GetMapping("/{siteId}/equipments")
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isSiteOwnedBy(#siteId, authentication)")
  @Operation(
      summary = "List the equipment of a site",
      description = "Lists the equipment installed at one of the authenticated owner's sites.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentResource.class)))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller")
  })
  public ResponseEntity<?> getEquipmentBySite(
      @PathVariable
      @Parameter(description = "Site unique identifier", example = "1", required = true)
      Long siteId,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    if (siteQueryService.handle(new GetSiteByIdQuery(siteId, ownerId)).isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(SITE_RESOURCE, String.valueOf(siteId)));
    }
    var equipment = equipmentQueryService.handle(new GetEquipmentBySiteQuery(siteId, ownerId));
    return ResponseEntity.ok(equipment.stream()
        .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
        .toList());
  }

  /**
   * Replaces the editable details of one of the authenticated owner's sites.
   *
   * @param siteId The site identifier
   * @param resource The {@link UpdateSiteResource} payload
   * @param authentication The authenticated principal, used as a fallback identity source
   * @return The updated {@link SiteResource}, with 200 OK
   */
  @PutMapping(value = "/{siteId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE') and @assetAccessEvaluator.isSiteOwnedBy(#siteId, authentication)")
  @Operation(
      summary = "Update a site",
      description = "Replaces the name, address, contact name and contact phone number of one of the "
          + "authenticated owner's sites. The owner of a site never changes.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Site updated",
          content = @Content(schema = @Schema(implementation = SiteResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "The site exists but belongs to another owner")
  })
  public ResponseEntity<?> updateSite(
      @PathVariable
      @Parameter(description = "Site unique identifier", example = "1", required = true)
      Long siteId,
      @Valid @RequestBody UpdateSiteResource resource,
      Authentication authentication) {
    var ownerId = ownerIdentityResolver.resolveOwnerIdOrNull(authentication);
    if (ownerId == null) {
      return noOwnerProfile();
    }
    var command = UpdateSiteInfoCommandFromResourceAssembler.toCommandFromResource(siteId, ownerId, resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        siteCommandService.handle(command),
        SiteResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Builds the 403 returned when a request carries no owner identity this context can act on.
   *
   * <p>Reported as access denied rather than as a validation failure: the payload may be perfectly
   * fine, there is simply nobody behind it.</p>
   *
   * @return the 403 response
   */
  private static ResponseEntity<?> noOwnerProfile() {
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no site or equipment"));
  }
}
