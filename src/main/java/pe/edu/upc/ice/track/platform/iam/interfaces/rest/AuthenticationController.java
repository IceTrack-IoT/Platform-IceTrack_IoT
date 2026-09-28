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
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CompleteGoogleOwnerRegistrationResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.CompleteGoogleTechnicianRegistrationResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithGoogleResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignInWithLocalResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpOwnerResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.SignUpTechnicianResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.UserResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.CompleteGoogleOwnerRegistrationCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.CompleteGoogleTechnicianRegistrationCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignInByGoogleCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignInByLocalCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignUpOwnerCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.SignUpTechnicianCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * AuthenticationController
 * <p>
 *     This controller is responsible for handling authentication requests.
 *     It exposes six endpoints:
 *     <ul>
 *         <li>POST /api/v1/authentication/sign-in/local</li>
 *         <li>POST /api/v1/authentication/sign-up/owner</li>
 *         <li>POST /api/v1/authentication/sign-up/technician</li>
 *         <li>POST /api/v1/authentication/google/verify</li>
 *         <li>POST /api/v1/authentication/google/complete-registration/owner</li>
 *         <li>POST /api/v1/authentication/google/complete-registration/technician</li>
 *     </ul>
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
