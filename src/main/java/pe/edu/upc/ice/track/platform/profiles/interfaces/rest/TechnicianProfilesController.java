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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.TechnicianCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetPublicTechnicianDirectoryQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianMetricsQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianDirectorySort;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileAccessEvaluator;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileView;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.PagedTechnicianPublicProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TechnicianProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TechnicianPublicProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateTechnicianResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.TechnicianProfileResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.TechnicianPublicProfileResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.UpdateTechnicianCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
  private final ProfileAccessEvaluator profileAccessEvaluator;

  /**
   * Constructor
   * @param technicianQueryService   The {@link TechnicianQueryService} instance
   * @param technicianCommandService The {@link TechnicianCommandService} instance
   * @param profileAccessEvaluator   The {@link ProfileAccessEvaluator} deciding what a caller may read
   */
  public TechnicianProfilesController(
      TechnicianQueryService technicianQueryService,
      TechnicianCommandService technicianCommandService,
      ProfileAccessEvaluator profileAccessEvaluator) {
    this.technicianQueryService = technicianQueryService;
    this.technicianCommandService = technicianCommandService;
    this.profileAccessEvaluator = profileAccessEvaluator;
  }

  /**
   * Get the technician directory
   *
   * <p>Owners browse it to choose whom to assign work to. Personal data (email, phone, home
   * address) is only included for technicians currently working on one of the caller's requests
   *
   * @param authentication The authenticated principal
   * @return A list of {@link TechnicianProfileResource} resources, trimmed to what the caller may see
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get all technicians",
      description = "Retrieves the technician directory for owners. Email, phone and address are null "
          + "except for technicians actively assigned to one of the caller's service requests.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technicians found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = TechnicianProfileResource.class)))
      )
  })
  public ResponseEntity<List<TechnicianProfileResource>> getAllTechnicians(Authentication authentication) {
    var technicians = technicianQueryService.handle(new GetAllTechniciansQuery());
    var technicianResources = technicians.stream()
        .map(technician -> toVisibleResource(Optional.of(technician), authentication))
        .flatMap(Optional::stream)
        .toList();
    return ResponseEntity.ok(technicianResources);
  }

  /**
   * Get one page of the public technician directory
   *
   * <p>The public profile of every technician, with its statistics, in one paged read, so an owner
   * choosing whom to assign work to does not ask technician by technician. Personal data - email,
   * phone, home address - is never part of it.</p>
   *
   * @param search Case-insensitive text matched against the full name or the speciality; optional
   * @param speciality Restrict to technicians of exactly this speciality; optional
   * @param minRating Restrict to technicians rated at least this much, sent as {@code min_rating}; optional
   * @param sort {@code name}, {@code rating} or {@code completed_services}; defaults to {@code name}
   * @param page Zero-based page index; defaults to 0
   * @param size Page size, capped at 100; defaults to 20
   * @return A {@link PagedTechnicianPublicProfileResource}
   */
  @GetMapping("/public")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get the public technician directory",
      description = "Lists the public profile of the technicians - name, speciality, certification number, "
          + "completed_services and average_rating - one page at a time. Optionally searched by name or "
          + "speciality, filtered by an exact speciality and by a minimum rating, which leaves out technicians "
          + "not rated yet, and ordered by name, by rating (best first, unrated last) or by completed services "
          + "(most first). Email, phone and address are never included. A size above 100 is capped to 100.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technicians found",
          content = @Content(schema = @Schema(implementation = PagedTechnicianPublicProfileResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR - unknown sort, min_rating outside 0 to 5, "
          + "negative page or size below 1"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner")
  })
  public ResponseEntity<PagedTechnicianPublicProfileResource> getPublicTechnicianDirectory(
      @RequestParam(required = false)
      @Parameter(description = "Case-insensitive search matching the full name or the speciality", example = "jane")
      String search,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to technicians of exactly this speciality", example = "Refrigeration")
      String speciality,
      @RequestParam(name = "min_rating", required = false)
      @Parameter(description = "Restrict to technicians rated at least this much, from 0 to 5", example = "4")
      Double minRating,
      @RequestParam(required = false)
      @Parameter(description = "Order of the directory: name, rating or completed_services", example = "rating")
      String sort,
      @RequestParam(defaultValue = "0")
      @Parameter(description = "Zero-based page index", example = "0")
      Integer page,
      @RequestParam(defaultValue = "20")
      @Parameter(description = "Page size, capped at 100", example = "20")
      Integer size
  ) {
    var query = new GetPublicTechnicianDirectoryQuery(search, speciality, minRating, toDirectorySort(sort), page, size);
    return ResponseEntity.ok(
        TechnicianPublicProfileResourceFromEntityAssembler.toPagedResourceFromPage(technicianQueryService.handle(query)));
  }

  /**
   * Get a technician profile by ID
   * @param technicianId The technician profile ID
   * @param authentication The authenticated principal
   * @return A {@link TechnicianProfileResource} resource for the technician, trimmed to what the
   *         caller may see, or 404 when the caller may not see it at all
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
      Long technicianId,
      Authentication authentication
  ) {
    return toVisibleResource(technicianQueryService.handle(new GetTechnicianByIdQuery(technicianId)), authentication)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(TECHNICIAN_RESOURCE, technicianId.toString())));
  }

  /**
   * Get the public profile of a technician
   *
   * <p>What any owner may see of a technician when choosing whom to assign work to: professional
   * data plus the completed services and average rating projected from Service Request Management.
   * Personal data - email, phone, home address - is never part of it.</p>
   *
   * @param technicianId The technician profile ID
   * @return A {@link TechnicianPublicProfileResource}, or 404 when no technician carries that ID
   */
  @GetMapping("/{technicianId}/public")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Get the public profile of a technician",
      description = "Retrieves a technician's name, speciality and certification number together with "
          + "completed_services and average_rating. average_rating is null, and rating_available false, until the "
          + "technician's first review. Email, phone and address are never included. The statistics are a read "
          + "model updated shortly after each completion or review, so they may lag a few seconds behind.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technician found",
          content = @Content(schema = @Schema(implementation = TechnicianPublicProfileResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "TECHNICIAN_NOT_FOUND - no technician carries that ID")
  })
  public ResponseEntity<?> getTechnicianPublicProfile(
      @PathVariable
      @Parameter(description = "Technician profile unique identifier", example = "7", required = true)
      Long technicianId
  ) {
    return technicianQueryService.handle(new GetTechnicianByIdQuery(technicianId))
        .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(
            TechnicianPublicProfileResourceFromEntityAssembler.toResourceFromEntity(
                profile, technicianQueryService.handle(new GetTechnicianMetricsQuery(technicianId)))))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(TECHNICIAN_RESOURCE, technicianId.toString())));
  }

  /**
   * Get the technician profile bound to a platform account
   * @param userId The platform account ID
   * @param authentication The authenticated principal
   * @return A {@link TechnicianProfileResource} resource for the technician, trimmed to what the
   *         caller may see, or 404 when the caller may not see it at all
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
      Long userId,
      Authentication authentication
  ) {
    return toVisibleResource(
            technicianQueryService.handle(new GetTechnicianByUserIdQuery(new UserId(userId))), authentication)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound(TECHNICIAN_RESOURCE, "user " + userId)));
  }

  /**
   * Reads the {@code sort} parameter of the public directory.
   *
   * @throws IllegalArgumentException for a value other than {@code name}, {@code rating} or
   *                                  {@code completed_services}, answered with 400 by
   *                                  {@code GlobalExceptionHandler}
   */
  private static TechnicianDirectorySort toDirectorySort(String sort) {
    if (sort == null || sort.isBlank()) {
      return TechnicianDirectorySort.NAME;
    }
    return switch (sort.strip().toLowerCase(Locale.ROOT)) {
      case "name" -> TechnicianDirectorySort.NAME;
      case "rating" -> TechnicianDirectorySort.RATING;
      case "completed_services" -> TechnicianDirectorySort.COMPLETED_SERVICES;
      default -> throw new IllegalArgumentException(
          "Parameter 'sort' must be one of [name, rating, completed_services]");
    };
  }

  /**
   * Trims a technician profile to what the caller may see: all of it for the
   * technician itself, contact channels for an owner it is working for, professional data only for
   * any other owner, nothing for another technician.
   */
  private Optional<TechnicianProfileResource> toVisibleResource(
      Optional<TechnicianProfile> technician, Authentication authentication) {
    return technician.flatMap(profile -> {
      var view = profileAccessEvaluator.technicianProfileViewFor(profile, authentication);
      return view == ProfileView.NONE
          ? Optional.empty()
          : Optional.of(TechnicianProfileResourceFromEntityAssembler.toResourceFromEntity(profile, view));
    });
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
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_TECHNICIAN + "') and @profileAccessEvaluator.isTechnicianSelf(#technicianId, authentication)")
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
