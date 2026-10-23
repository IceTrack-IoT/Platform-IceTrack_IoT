package pe.edu.upc.ice.track.platform.service.request.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ServiceRequestCommandService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.InterventionRegistration;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.InterventionResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.RegisterInterventionResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.InterventionResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.RegisterInterventionCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

/**
 * REST controller through which a technician registers the intervention that completes a service
 * request.
 *
 * <p>Submission is idempotent on {@code client_generated_id}: a device retrying a submission it never
 * saw acknowledged is answered with the intervention already registered and {@code 200 OK}, instead
 * of the {@code 201 Created} of the first submission. That status difference is decided here, from
 * the {@link InterventionRegistration} the command service returns, which is why this endpoint does
 * not go through {@code ResponseEntityAssembler}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/interventions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Interventions", description = "Technical intervention endpoints: idempotent registration of the work that completes a service request")
public class InterventionsController {

  private final ServiceRequestCommandService serviceRequestCommandService;

  /**
   * Constructor
   *
   * @param serviceRequestCommandService The {@link ServiceRequestCommandService} instance
   */
  public InterventionsController(ServiceRequestCommandService serviceRequestCommandService) {
    this.serviceRequestCommandService = serviceRequestCommandService;
  }

  /**
   * Registers the intervention that completes a service request assigned to the authenticated
   * technician.
   *
   * @param resource       The {@link RegisterInterventionResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The {@link InterventionResource}, with 201 Created on a new registration or 200 OK on a
   *         replay
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('TECHNICIAN_ROLE')")
  @Operation(
      summary = "Register an intervention",
      description = "Registers the technical report of an IN_PROGRESS request assigned to the "
          + "authenticated technician and completes the request. Re-submitting the same "
          + "client_generated_id for the same request returns the registered intervention with 200 "
          + "instead of failing.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Intervention registered and service request completed",
          content = @Content(schema = @Schema(implementation = InterventionResource.class))),
      @ApiResponse(
          responseCode = "200",
          description = "Replay of an intervention already registered",
          content = @Content(schema = @Schema(implementation = InterventionResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid report, such as a blank diagnosis or a completion before the start"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not a technician"),
      @ApiResponse(responseCode = "404", description = "No such service request is assigned to the caller"),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION, DUPLICATE_RESOURCE_CONFLICT for a client_generated_id of another request, or CONCURRENT_MODIFICATION_CONFLICT")
  })
  public ResponseEntity<?> registerIntervention(
      @Valid @RequestBody RegisterInterventionResource resource,
      Authentication authentication) {
    var command = RegisterInterventionCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource);
    return switch (serviceRequestCommandService.handle(command)) {
      case Result.Success<InterventionRegistration, ApplicationError> success -> new ResponseEntity<>(
          InterventionResourceFromEntityAssembler.toResourceFromEntity(success.value().serviceRequest()),
          success.value().replayed() ? HttpStatus.OK : HttpStatus.CREATED);
      case Result.Failure<InterventionRegistration, ApplicationError> failure ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
    };
  }
}
