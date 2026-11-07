package pe.edu.upc.ice.track.platform.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByUsernameQuery;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.UserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import java.util.List;

/**
 * REST controller that exposes IAM user resources.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User management endpoints")
public class UsersController {
  private final UserQueryService userQueryService;

  public UsersController(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  /**
   * Retrieves the caller's own account, as a list.
   *
   * <p>There is no account directory: listing every username and role to any
   * authenticated caller made account enumeration trivial. The list shape is kept so existing
   * clients keep working; {@code /api/v1/authentication/me} is the preferred way to read the
   * current account.</p>
   *
   * @param authentication the authenticated principal
   * @return a list holding the caller's own user resource
   * @see UserResource
   */
  @GetMapping
  @Operation(
      summary = "Get my account",
      description = "Returns a list holding only the authenticated caller's own account. "
          + "Other accounts are never listed.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Users retrieved successfully",
          content = @Content(schema = @Schema(implementation = UserResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
  })
  public ResponseEntity<List<UserResource>> getAllUsers(Authentication authentication) {
    var userResources = userQueryService.handle(new GetUserByUsernameQuery(authentication.getName())).stream()
        .map(UserResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(userResources);
  }

  /**
   * Retrieves the caller's own account by identifier.
   *
   * @param userId user identifier
   * @param authentication the authenticated principal
   * @return user resource when it is the caller's own account, 404 otherwise (another account is
   *         answered exactly like a missing one, so identifiers cannot be harvested)
   * @see UserResource
   */
  @GetMapping(value = "/{userId}")
  @Operation(
      summary = "Get user by ID",
      description = "Retrieves the caller's own account by its unique identifier; any other "
          + "identifier answers 404.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "User retrieved successfully",
          content = @Content(schema = @Schema(implementation = UserResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public ResponseEntity<UserResource> getUserById(
      @PathVariable
      @Parameter(
          description = "Unique user identifier",
          example = "1",
          required = true
      )
      Long userId,
      Authentication authentication
  ) {
    return userQueryService.handle(new GetUserByUsernameQuery(authentication.getName()))
        .filter(user -> user.getId().equals(userId))
        .map(user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)))
        .orElse(ResponseEntity.notFound().build());
  }
}
