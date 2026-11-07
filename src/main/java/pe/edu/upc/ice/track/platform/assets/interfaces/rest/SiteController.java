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
import java.util.List;
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
import pe.edu.upc.ice.track.platform.assets.application.commandservices.AddressValidationCommandService;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.SiteQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentBySiteQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSitesByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.AddressNotVerifiedResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.AddressValidationResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterSiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ValidateAddressResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.AddressValidationResourceFromResultAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.ValidateAddressCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.AddressValidationResult;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.SiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteAddressResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteResource;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.RegisterSiteCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.SiteResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.UpdateSiteAddressCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform.UpdateSiteInfoCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller exposing the sites of an owner.
 *
 * <p>Every endpoint acts on the authenticated caller: the controller only hands the principal's
 * username to the command and query services, which resolve it to an owner and apply the
 * ownership rules. No request body carries an owner and no endpoint accepts one as a parameter, so
 * a client cannot register a site for somebody else or read one that is not its own.</p>
 *
 * <p>Every endpoint declares its roles through method security, so an account outside them is
 * answered with 403 before a service is reached. Registration, the listing and both updates are
 * {@code OWNER_ROLE} only. Fetching a site and listing its equipment are also open to
 * {@code TECHNICIAN_ROLE}: the role the caller acts in is handed to the query services, which do
 * not scope a technician by ownership, since a technician is sent to sites of any owner.</p>
 *
 * <p>This controller holds no business logic: it converts a payload into a command through its
 * atomic assembler, delegates to a command or query service, and converts the outcome into a
 * resource. The single decision it does make is which HTTP status a result maps to,
 * and that mapping is shared across the platform by
 * {@link pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/assets/sites", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sites", description = "Ice track site endpoints: registration and listing of the premises where equipment is installed")
public class SiteController {

  private static final String SITE_RESOURCE = "Site";

  private final SiteQueryService siteQueryService;
  private final SiteCommandService siteCommandService;
  private final EquipmentQueryService equipmentQueryService;
  private final AddressValidationCommandService addressValidationCommandService;

  /**
   * Constructor
   *
   * @param siteQueryService The {@link SiteQueryService} instance
   * @param siteCommandService The {@link SiteCommandService} instance
   * @param equipmentQueryService The {@link EquipmentQueryService} instance, used to list the
   *                              equipment installed at a site
   * @param addressValidationCommandService The {@link AddressValidationCommandService} instance, which
   *                              verifies addresses before a site is saved
   */
  public SiteController(
      SiteQueryService siteQueryService,
      SiteCommandService siteCommandService,
      EquipmentQueryService equipmentQueryService,
      AddressValidationCommandService addressValidationCommandService) {
    this.siteQueryService = siteQueryService;
    this.siteCommandService = siteCommandService;
    this.equipmentQueryService = equipmentQueryService;
    this.addressValidationCommandService = addressValidationCommandService;
  }

  /**
   * Verifies an address with Google Maps before a site is saved.
   *
   * <p>Nothing is persisted. A verified address is answered with a token that is valid for 15
   * minutes and must be presented when the site is registered or moved.</p>
   *
   * @param resource The {@link ValidateAddressResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The verified address and its token with 200 OK, or the reason it was refused
   */
  @PostMapping(value = "/validate-address", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Verify a site address",
      description = "Verifies an address with Google Maps, restricted to Metropolitan Lima and the Constitutional "
          + "Province of Callao. Returns the normalized address, its coordinates and district, and a validation_token "
          + "valid for 15 minutes. Nothing is saved. Callers are limited to 20 validations per minute.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Address verified",
          content = @Content(schema = @Schema(implementation = AddressValidationResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a missing address"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(
          responseCode = "422",
          description = "ADDRESS_OUT_OF_COVERAGE, or ADDRESS_NOT_VERIFIED with up to five candidates",
          content = @Content(schema = @Schema(implementation = AddressNotVerifiedResource.class))),
      @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS - more than 20 validations in a minute"),
      @ApiResponse(responseCode = "503", description = "GEOCODING_UNAVAILABLE - the maps service failed or timed out; Retry-After is 30")
  })
  public ResponseEntity<?> validateAddress(
      @Valid @RequestBody ValidateAddressResource resource,
      Authentication authentication) {
    var command = ValidateAddressCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource);
    return switch (addressValidationCommandService.handle(command)) {
      case Result.Success<AddressValidationResult, ApplicationError> success ->
          ResponseEntity.ok(AddressValidationResourceFromResultAssembler.toResource(success.value()));
      case Result.Failure<AddressValidationResult, ApplicationError> failure -> validationFailure(failure.error());
    };
  }

  /**
   * Maps a failed address validation to its response, adding the candidates and the retry hints.
   *
   * @param error the failure reported by the command service
   * @return the response for the failure
   */
  private static ResponseEntity<?> validationFailure(ApplicationError error) {
    switch (error.code()) {
      case "ADDRESS_NOT_VERIFIED" -> {
        var candidates = error.details() == null || error.details().isBlank()
            ? List.<String>of()
            : List.of(error.details().split("\n"));
        return ResponseEntity.unprocessableContent()
            .body(new AddressNotVerifiedResource(error.code(), error.message(), candidates));
      }
      case "GEOCODING_UNAVAILABLE" -> {
        var response = ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
        return ResponseEntity.status(response.getStatusCode()).header("Retry-After", "30").body(response.getBody());
      }
      case "TOO_MANY_REQUESTS" -> {
        var response = ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
        return ResponseEntity.status(response.getStatusCode()).header("Retry-After", "60").body(response.getBody());
      }
      default -> {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
      }
    }
  }

  /**
   * Registers a new site for the authenticated owner.
   *
   * @param resource The {@link RegisterSiteResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The registered {@link SiteResource}, with 201 Created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Register a site",
      description = "Registers a new site for the authenticated owner at the address carried by a validation_token "
          + "issued by POST /validate-address. The owner is taken from the authenticated identity and cannot be "
          + "supplied in the payload. An optional pin must lie within 150 meters of the verified point. The site "
          + "is saved as RESOLVED; the maps service is not queried.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Site registered",
          content = @Content(schema = @Schema(implementation = SiteResource.class))),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, ADDRESS_VALIDATION_REQUIRED (missing, invalid, expired or foreign token), or PIN_OUT_OF_RANGE"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile")
  })
  public ResponseEntity<?> registerSite(
      @Valid @RequestBody RegisterSiteResource resource,
      Authentication authentication) {
    var command = RegisterSiteCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        siteCommandService.handle(command),
        SiteResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Lists every site belonging to the authenticated owner.
   *
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The {@link SiteResource} list, with 200 OK
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
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
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner")
  })
  public ResponseEntity<?> getMySites(Authentication authentication) {
    var sites = siteQueryService.handle(new GetSitesByOwnerQuery(authentication.getName()));
    return ResponseEntity.ok(sites.stream().map(SiteResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  /**
   * Fetches one of the authenticated owner's sites.
   *
   * <p>A site that does not exist and a site owned by somebody else both answer 404, so this
   * endpoint cannot be used to discover which site identifiers are real.</p>
   *
   * @param siteId The site identifier
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The {@link SiteResource}, or 404 when it is not reachable by the caller
   */
  @GetMapping("/{siteId}")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "Get a site by ID",
      description = "Retrieves a site by its unique identifier. Owners can only read their own sites; "
          + "technicians can only read sites where they hold an active service request. "
          + "Any other site answers 404.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Site found",
          content = @Content(schema = @Schema(implementation = SiteResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller")
  })
  public ResponseEntity<?> getSiteById(
      @PathVariable
      @Parameter(description = "Site unique identifier", example = "1", required = true)
      Long siteId,
      Authentication authentication) {
    var site = siteQueryService.handle(new GetSiteByIdQuery(
        siteId,
        authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication)));
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
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The {@link EquipmentResource} list, or 404 when the site is not reachable by the caller
   */
  @GetMapping("/{siteId}/equipments")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "List the equipment of a site",
      description = "Lists the equipment installed at a site. Owners can only list their own sites; "
          + "technicians only see the units of the site they hold an active service request on.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Equipment found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentResource.class)))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller")
  })
  public ResponseEntity<?> getEquipmentBySite(
      @PathVariable
      @Parameter(description = "Site unique identifier", example = "1", required = true)
      Long siteId,
      Authentication authentication) {
    var username = authentication.getName();
    var callerRole = CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication);
    if (siteQueryService.handle(new GetSiteByIdQuery(siteId, username, callerRole)).isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(SITE_RESOURCE, String.valueOf(siteId)));
    }
    var equipment = equipmentQueryService.handle(new GetEquipmentBySiteQuery(siteId, username, callerRole));
    return ResponseEntity.ok(equipment.stream()
        .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
        .toList());
  }

  /**
   * Replaces the editable details of one of the authenticated owner's sites.
   *
   * @param siteId The site identifier
   * @param resource The {@link UpdateSiteResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The updated {@link SiteResource}, with 200 OK
   */
  @PutMapping(value = "/{siteId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Update a site",
      description = "Replaces the name, contact name and contact phone number of one of the authenticated "
          + "owner's sites. The address and coordinates are not changed here: use PUT /{siteId}/address with a "
          + "validation_token. The owner of a site never changes.",
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
    var command = UpdateSiteInfoCommandFromResourceAssembler.toCommandFromResource(
        siteId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        siteCommandService.handle(command),
        SiteResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Moves one of the authenticated owner's sites to an address verified beforehand.
   *
   * @param siteId The site identifier
   * @param resource The {@link UpdateSiteAddressResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The updated {@link SiteResource} with its verified location, with 200 OK
   */
  @PutMapping(value = "/{siteId}/address", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Change the address of a site",
      description = "Moves one of the authenticated owner's sites to the address carried by a validation_token "
          + "issued by POST /validate-address, keeping its equipment. An optional pin must lie within 150 meters "
          + "of the verified point. The site is answered RESOLVED with its new location.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Address changed and located",
          content = @Content(schema = @Schema(implementation = SiteResource.class))),
      @ApiResponse(responseCode = "400", description = "ADDRESS_VALIDATION_REQUIRED or PIN_OUT_OF_RANGE"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account has no owner profile"),
      @ApiResponse(responseCode = "404", description = "No such site is reachable by the caller")
  })
  public ResponseEntity<?> updateSiteAddress(
      @PathVariable
      @Parameter(description = "Site unique identifier", example = "1", required = true)
      Long siteId,
      @Valid @RequestBody UpdateSiteAddressResource resource,
      Authentication authentication) {
    var command = UpdateSiteAddressCommandFromResourceAssembler.toCommandFromResource(
        siteId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        siteCommandService.handle(command),
        SiteResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
