package pe.edu.upc.ice.track.platform.profiles.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.ProfileQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.ProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.util.Collections;
import java.util.List;

/**
 * REST controller that exposes profile retrieval endpoints.
 *
 * <p>There is deliberately no creation endpoint: a profile is only ever created together with
 * its platform account, through the IAM registration flows and the profiles ACL facade, so that
 * no profile can exist without an account or without a definitive role.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Profile management endpoints")
public class ProfilesController {
  private final ProfileQueryService profileQueryService;

  /**
   * Constructor
   * @param profileQueryService The {@link ProfileQueryService} instance
   */
  public ProfilesController(ProfileQueryService profileQueryService) {
    this.profileQueryService = profileQueryService;
  }

  /**
   * Get a profile by ID
   * @param profileId The profile ID
   * @return A {@link ProfileResource} resource for the profile
   */
  @GetMapping("/{profileId}")
  @Operation(
      summary = "Get profile by ID",
      description = "Retrieves a specific user profile's information by unique identifier."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Profile found",
          content = @Content(schema = @Schema(implementation = ProfileResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Profile not found")
  })
  public ResponseEntity<?> getProfileById(
      @PathVariable
      @Parameter(description = "Profile unique identifier", example = "1", required = true)
      Long profileId
  ) {
    var getProfileByIdQuery = new GetProfileByIdQuery(profileId);
    var profile = profileQueryService.handle(getProfileByIdQuery);
    if (profile.isEmpty()) {
      var error = ApplicationError.notFound("Profile", profileId.toString());
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
    }
    var profileEntity = profile.get();
    var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profileEntity);
    return ResponseEntity.ok(profileResource);
  }

  /**
   * Get all profiles
   * @return A list of {@link ProfileResource} resources for all profiles
   */
  @GetMapping
  @Operation(
      summary = "Get all profiles",
      description = "Retrieves a list of all user profiles in the system."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Profiles found",
          content = @Content(schema = @Schema(implementation = ProfileResource.class))
      )
  })
  public ResponseEntity<List<ProfileResource>> getAllProfiles() {
    var profiles = profileQueryService.handle(new GetAllProfilesQuery());
    if (profiles.isEmpty()) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    var profileResources = profiles.stream()
        .map(ProfileResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(profileResources);
  }

}
