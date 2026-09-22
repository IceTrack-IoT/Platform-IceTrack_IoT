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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetAllUsersQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByIdQuery;
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
   * Retrieves all users.
   *
   * @return list of user resources
   * @see UserResource
   */
  @GetMapping
  @Operation(
      summary = "Get all users",
      description = "Retrieves a list of all users in the system with their roles.",
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
  public ResponseEntity<List<UserResource>> getAllUsers() {
    var getAllUsersQuery = new GetAllUsersQuery();
    var users = userQueryService.handle(getAllUsersQuery);
    var userResources = users.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(userResources);
  }

  /**
   * Retrieves a user by identifier.
   *
   * @param userId user identifier
   * @return user resource when found
   * @see UserResource
   */
  @GetMapping(value = "/{userId}")
  @Operation(
      summary = "Get user by ID",
      description = "Retrieves a specific user's information by unique identifier.",
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
      Long userId
  ) {
    var getUserByIdQuery = new GetUserByIdQuery(userId);
    var user = userQueryService.handle(getUserByIdQuery);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
    return ResponseEntity.ok(userResource);
  }
}
