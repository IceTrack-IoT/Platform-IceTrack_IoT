package pe.edu.upc.ice.track.platform.profiles.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.SessionContextQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetSessionContextQuery;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.SessionContextResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform.SessionContextResourceFromResultAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

/**
 * REST controller answering the session bootstrap: everything a client shell needs after sign-in or a
 * page reload, in one request instead of several dependent ones.
 *
 * <p>Lives in the Profiles context because most of what it answers is Profiles data (the owner
 * profile and the dashboard configuration); the account comes from IAM and the counts from Assets,
 * both through their published facades. It never answers a token and does not replace
 * {@code GET /api/v1/authentication/me}, which IAM keeps for session restoration.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Session", description = "Session bootstrap for the signed-in account")
public class SessionContextController {

  private final SessionContextQueryService sessionContextQueryService;

  /**
   * Constructor
   *
   * @param sessionContextQueryService the {@link SessionContextQueryService} instance
   */
  public SessionContextController(SessionContextQueryService sessionContextQueryService) {
    this.sessionContextQueryService = sessionContextQueryService;
  }

  /**
   * Answers the session context of the authenticated account.
   *
   * @param authentication the authenticated principal
   * @return the {@link SessionContextResource} with 200 OK, never cached
   */
  @GetMapping("/context")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get my session context",
      description = "Answers, in one read, the signed-in account (the shape of GET /authentication/me), its owner "
          + "profile with the address parts (null for a technician), its dashboard configuration (null while not "
          + "set up), how many sites and units it owns (0 for a technician) and the server time, from which a "
          + "client computes its clock skew once. The answer carries Cache-Control: no-store and never a token.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Session context",
          content = @Content(schema = @Schema(implementation = SessionContextResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid, or the account "
          + "no longer exists")
  })
  public ResponseEntity<?> getSessionContext(Authentication authentication) {
    var context = sessionContextQueryService.handle(new GetSessionContextQuery(authentication.getName()));
    if (context.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.unauthorized("The authenticated account no longer exists"));
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(SessionContextResourceFromResultAssembler.toResourceFromResult(context.get()));
  }
}
