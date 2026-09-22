package pe.edu.upc.ice.track.platform.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithGoogleResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithLocalResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpWithLocalResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.UserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.ExchangeGoogleTokenCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignInByLocalCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignUpByLocalCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * AuthenticationController
 * <p>
 *     This controller is responsible for handling authentication requests.
 *     It exposes three endpoints:
 *     <ul>
 *         <li>POST /api/v1/authentication/sign-in</li>
 *         <li>POST /api/v1/authentication/sign-up</li>
 *         <li>POST /api/v1/authentication/sign-in-with-google</li>
 *     </ul>
 * </p>
 * <p>
 *     The Google endpoint implements the OAuth2 Token Exchange pattern: the frontend performs the
 *     Google login and posts the resulting OIDC {@code id_token}; the backend validates it against
 *     Google's JWKS and returns the platform's own bearer token. There is no separate Google
 *     sign-up endpoint - the first successful exchange registers the account.
 * </p>
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthenticationController {
  private final UserCommandService userCommandService;

  public AuthenticationController(UserCommandService userCommandService) {
    this.userCommandService = userCommandService;
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
   * Registers a new user with local credentials.
   *
   * @param resource the sign-up payload
   * @return the created user resource
   * @see UserResource
   */
  @PostMapping(value = "/sign-up", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Sign up with local credentials",
      description = "Registers a new user with username, password and optional roles."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "User registered successfully",
          content = @Content(schema = @Schema(implementation = UserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "409", description = "Conflict - username or email already taken")
  })
  public ResponseEntity<?> signUp(@Valid @RequestBody SignUpWithLocalResource resource) {
    var signUpByLocalCommand = SignUpByLocalCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(signUpByLocalCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        UserResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Exchanges a Google OIDC id_token for a platform bearer token.
   *
   * <p>The token is validated against Google's JWKS with Spring Security's {@code NimbusJwtDecoder}.
   * On the first successful exchange the account is registered, which in turn triggers the
   * creation of the matching profile in the {@code profiles} bounded context.</p>
   *
   * @param resource the token exchange payload carrying the Google id_token
   * @return the authenticated user together with the issued bearer token
   * @see AuthenticatedUserResource
   */
  @PostMapping(value = "/sign-in/google", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Exchange a Google id_token for a platform bearer token",
      description = "Validates the Google OIDC id_token obtained by the frontend against Google's JWKS, "
          + "registers the account on first contact, and returns the platform bearer token."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Token exchanged successfully",
          content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "The Google id_token is missing, malformed, expired or not addressed to this application"),
      @ApiResponse(responseCode = "409", description = "Conflict - the Google email collides with an existing username")
  })
  public ResponseEntity<?> signInWithGoogle(@Valid @RequestBody SignInWithGoogleResource resource) {
    var exchangeGoogleTokenCommand = ExchangeGoogleTokenCommandFromResourceAssembler.toCommandFromResource(resource);
    var result = userCommandService.handle(exchangeGoogleTokenCommand);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result,
        AuthenticationController::toAuthenticatedUserResource,
        HttpStatus.OK);
  }

  /**
   * Maps an authentication outcome onto its REST representation.
   *
   * @param authenticatedUser the authenticated user paired with the issued bearer token
   * @return the authenticated user resource
   */
  private static AuthenticatedUserResource toAuthenticatedUserResource(ImmutablePair<User, String> authenticatedUser) {
    return AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(
        authenticatedUser.getLeft(),
        authenticatedUser.getRight());
  }
}
