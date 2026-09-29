package pe.edu.upc.ice.track.platform.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetCurrentUserQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.SessionTokens;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CompleteGoogleOwnerRegistrationResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CompleteGoogleTechnicianRegistrationResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CurrentUserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.RefreshTokenResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithGoogleResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithLocalResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpOwnerResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpTechnicianResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.UserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.CompleteGoogleOwnerRegistrationCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.CompleteGoogleTechnicianRegistrationCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.CurrentUserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.RefreshTokenCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignInByGoogleCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignInByLocalCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignOutCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignUpOwnerCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignUpTechnicianCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * AuthenticationController
 * <p>
 *     This controller is responsible for handling authentication requests.
 *     It exposes nine endpoints:
 *     <ul>
 *         <li>POST /api/v1/authentication/sign-in/local</li>
 *         <li>POST /api/v1/authentication/sign-up/owner</li>
 *         <li>POST /api/v1/authentication/sign-up/technician</li>
 *         <li>POST /api/v1/authentication/google/verify</li>
 *         <li>POST /api/v1/authentication/google/complete-registration/owner</li>
 *         <li>POST /api/v1/authentication/google/complete-registration/technician</li>
 *         <li>POST /api/v1/authentication/refresh-token</li>
 *         <li>POST /api/v1/authentication/logout</li>
 *         <li>GET /api/v1/authentication/me</li>
 *     </ul>
 * </p>
 * <p>
 *     Every sign-in returns a short-lived access token together with a single-use refresh token.
 *     {@code /refresh-token} rotates the refresh token and issues a new pair; {@code /logout}
 *     discards it. Both are reachable anonymously, since the refresh token is itself the
 *     credential and the access token may already be expired. {@code /me} is the only endpoint of
 *     this controller that requires a valid access token.
 * </p>
 * <p>
 *     Registration endpoints are role explicit: the role is implied by the path and never sent by
 *     the client, and each payload only carries the attributes of that role. Each endpoint maps to
 *     exactly one typed command.
 * </p>
 * <p>
 *     The Google endpoints implement the deferred registration strategy: the frontend performs the
 *     Google login and posts the resulting OIDC {@code id_token} to {@code /google/verify}. A
 *     registered account receives the platform's own bearer token; an unknown one receives a 404
 *     and must submit the onboarding form of the chosen role to
 *     {@code /google/complete-registration/{owner|technician}}.
 * </p>
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthenticationController {
  private final UserCommandService userCommandService;
  private final UserQueryService userQueryService;

  public AuthenticationController(UserCommandService userCommandService, UserQueryService userQueryService) {
    this.userCommandService = userCommandService;
    this.userQueryService = userQueryService;
  }

  /**
   * Authenticates a user with local credentials.
   *
   * @param resource the sign-in payload
   * @return the authenticated user together with the issued bearer token
   * @see AuthenticatedUserResource
   */
  @PostMapping(value = "/sign-in/local", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Sign in with local credentials",
      description = "Authenticates a user with username and password and returns the platform bearer token."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "User authenticated successfully",
          content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid credentials or malformed request"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "422", description = "Account is federated and cannot use local credentials")
  })
  public ResponseEntity<?> signIn(@Valid @RequestBody SignInWithLocalResource resource) {
    var signInByLocalCommand = SignInByLocalCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(signInByLocalCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        AuthenticationController::toAuthenticatedUserResource,
        HttpStatus.OK);
  }

  /**
   * Registers an ice track owner with local credentials.
   *
   * @param resource the owner sign-up payload
   * @return the created user resource
   * @see UserResource
   */
  @PostMapping(value = "/sign-up/owner", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Sign up an owner with local credentials",
      description = "Registers an account with OWNER_ROLE and creates its owner profile (RUC included) atomically."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Owner registered successfully",
          content = @Content(schema = @Schema(implementation = UserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a missing field or a RUC that is not 11 digits"),
      @ApiResponse(responseCode = "409", description = "Conflict - username, email or profile already taken")
  })
  public ResponseEntity<?> signUpOwner(@Valid @RequestBody SignUpOwnerResource resource) {
    var signUpOwnerCommand = SignUpOwnerCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(signUpOwnerCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        UserResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Registers an ice track maintenance technician with local credentials.
   *
   * @param resource the technician sign-up payload
   * @return the created user resource
   * @see UserResource
   */
  @PostMapping(value = "/sign-up/technician", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Sign up a technician with local credentials",
      description = "Registers an account with TECHNICIAN_ROLE and creates its technician profile (speciality and "
          + "certification number included) atomically."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Technician registered successfully",
          content = @Content(schema = @Schema(implementation = UserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a missing speciality or certification number"),
      @ApiResponse(responseCode = "409", description = "Conflict - username, email or profile already taken")
  })
  public ResponseEntity<?> signUpTechnician(@Valid @RequestBody SignUpTechnicianResource resource) {
    var signUpTechnicianCommand = SignUpTechnicianCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(signUpTechnicianCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        UserResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Verifies a Google OIDC id_token against the registered accounts - step one of the deferred
   * registration flow.
   *
   * <p>The token is validated against Google's JWKS with Spring Security's {@code NimbusJwtDecoder}.
   * A registered Google account is signed in. An unknown one is answered with
   * {@code 404 GOOGLE_ACCOUNT_NOT_FOUND} and nothing is persisted: the frontend must then show the
   * role selection onboarding form.</p>
   *
   * @param resource the payload carrying the Google id_token
   * @return the authenticated user together with the issued bearer token
   * @see AuthenticatedUserResource
   */
  @PostMapping(value = "/google/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Verify a Google id_token and sign in a registered account",
      description = "Validates the Google OIDC id_token obtained by the frontend. Returns the platform bearer "
          + "token when the Google account is registered, or 404 GOOGLE_ACCOUNT_NOT_FOUND when the onboarding "
          + "form must be completed first."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Google account registered; user signed in",
          content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "The Google id_token is missing, malformed, expired or not addressed to this application"),
      @ApiResponse(responseCode = "404", description = "GOOGLE_ACCOUNT_NOT_FOUND - onboarding completion is required")
  })
  public ResponseEntity<?> verifyGoogleAccount(@Valid @RequestBody SignInWithGoogleResource resource) {
    var signInByGoogleCommand = SignInByGoogleCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(signInByGoogleCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        AuthenticationController::toAuthenticatedUserResource,
        HttpStatus.OK);
  }

  /**
   * Completes the registration of a Google account as an ice track owner - step two of the
   * deferred registration flow.
   *
   * @param resource the Google id_token together with the owner onboarding form
   * @return the authenticated user together with the issued bearer token
   * @see AuthenticatedUserResource
   */
  @PostMapping(value = "/google/complete-registration/owner", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Complete the registration of a Google account as an owner",
      description = "Re-verifies the Google id_token, creates the account with OWNER_ROLE and its owner profile "
          + "atomically, then returns the platform bearer token. An already registered Google account is signed "
          + "in with its existing role."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Owner account and profile created; user signed in",
          content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid id_token or onboarding form, such as a RUC that is not 11 digits"),
      @ApiResponse(responseCode = "409", description = "Conflict - username, email or profile already taken")
  })
  public ResponseEntity<?> completeGoogleOwnerRegistration(@Valid @RequestBody CompleteGoogleOwnerRegistrationResource resource) {
    var command = CompleteGoogleOwnerRegistrationCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(command);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        AuthenticationController::toAuthenticatedUserResource,
        HttpStatus.CREATED);
  }

  /**
   * Completes the registration of a Google account as a maintenance technician - step two of the
   * deferred registration flow.
   *
   * @param resource the Google id_token together with the technician onboarding form
   * @return the authenticated user together with the issued bearer token
   * @see AuthenticatedUserResource
   */
  @PostMapping(value = "/google/complete-registration/technician", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Complete the registration of a Google account as a technician",
      description = "Re-verifies the Google id_token, creates the account with TECHNICIAN_ROLE and its technician "
          + "profile atomically, then returns the platform bearer token. An already registered Google account is "
          + "signed in with its existing role."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Technician account and profile created; user signed in",
          content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid id_token or onboarding form, such as a missing speciality or certification number"),
      @ApiResponse(responseCode = "409", description = "Conflict - username, email or profile already taken")
  })
  public ResponseEntity<?> completeGoogleTechnicianRegistration(
      @Valid @RequestBody CompleteGoogleTechnicianRegistrationResource resource) {
    var command = CompleteGoogleTechnicianRegistrationCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(command);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        AuthenticationController::toAuthenticatedUserResource,
        HttpStatus.CREATED);
  }

  /**
   * Exchanges a refresh token for a new access token and a new refresh token.
   *
   * <p>The presented refresh token is single use: it is revoked by this call, and the returned
   * refresh token must be used next. Presenting a refresh token a second time is treated as a
   * replay and revokes every session of the account.</p>
   *
   * @param resource the payload carrying the refresh token
   * @return the authenticated user together with the new session tokens
   * @see AuthenticatedUserResource
   */
  @PostMapping(value = "/refresh-token", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Refresh the session tokens",
      description = "Rotates the refresh token: revokes the presented one and returns a new access token and a new "
          + "refresh token. Replaying an already used refresh token revokes every session of the account."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Session refreshed",
          content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "The refresh token is missing"),
      @ApiResponse(responseCode = "401", description = "The refresh token is invalid, expired or revoked")
  })
  public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenResource resource) {
    var refreshTokenCommand = RefreshTokenCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(refreshTokenCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        AuthenticationController::toAuthenticatedUserResource,
        HttpStatus.OK);
  }

  /**
   * Ends the session bound to a refresh token.
   *
   * <p>Idempotent: an unknown or already discarded refresh token is also answered with 204. The
   * access token stays valid until it expires, so the client must discard it as well.</p>
   *
   * @param resource the payload carrying the refresh token of the session to end
   * @return an empty response
   */
  @PostMapping(value = "/logout")
  @Operation(
      summary = "Sign out",
      description = "Discards the refresh token so that it can no longer be exchanged. The short-lived access "
          + "token is stateless and remains valid until it expires; clients must discard it."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Signed out"),
      @ApiResponse(responseCode = "400", description = "The refresh token is missing")
  })
  public ResponseEntity<Void> signOut(@Valid @RequestBody(required = false) RefreshTokenResource resource) {
    var signOutCommand = SignOutCommandFromResourceAssembler.toCommandFromResource(resource);
    userCommandService.handle(signOutCommand);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns the account of the authenticated principal.
   *
   * @param principal the principal resolved from the bearer token, or {@code null} when none is
   *                  authenticated
   * @return the current user resource
   * @see CurrentUserResource
   */
  @GetMapping(value = "/me")
  @Operation(
      summary = "Get the current user",
      description = "Returns the identity and roles of the account the bearer token was issued to.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Current user retrieved successfully",
          content = @Content(schema = @Schema(implementation = CurrentUserResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
    // The security filter chain already rejects anonymous calls; this guard keeps the endpoint
    // safe should the principal ever be missing or of an unexpected type.
    if (principal == null || principal.getUsername().isBlank()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.unauthorized("No authenticated user"));
    }
    var getCurrentUserQuery = new GetCurrentUserQuery(principal.getUsername());
    var user = userQueryService.handle(getCurrentUserQuery);
    if (user.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.unauthorized("The authenticated account no longer exists"));
    }
    return ResponseEntity.ok(CurrentUserResourceFromEntityAssembler.toResourceFromEntity(user.get()));
  }

  /**
   * Maps an authentication outcome onto its REST representation.
   *
   * @param authenticatedUser the authenticated user paired with its session tokens
   * @return the authenticated user resource
   */
  private static AuthenticatedUserResource toAuthenticatedUserResource(ImmutablePair<User, SessionTokens> authenticatedUser) {
    return AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(
        authenticatedUser.getLeft(),
        authenticatedUser.getRight());
  }
}
