package pe.edu.upc.ice.track.platform.service.request.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ServiceRequestCommandService;
import pe.edu.upc.ice.track.platform.service.request.application.queryservices.ReviewQueryService;
import pe.edu.upc.ice.track.platform.service.request.application.queryservices.ServiceRequestQueryService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AcceptServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetReviewByServiceRequestIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestByIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListServiceRequestsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.AssignTechnicianResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.CancelServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.CreateServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.PagedServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.RejectServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ReviewResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceStatusResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.StartServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.AssignTechnicianCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.CancelServiceRequestCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.CreateServiceRequestCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.RejectServiceRequestCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.ReviewResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.ServiceRequestResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.StartServiceRequestCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing the lifecycle of service requests.
 *
 * <p>Every action is scoped to the authenticated caller: the controller only hands the principal's
 * username - and, for the reads open to both roles, the role it acts in - to the command and query
 * services, which resolve it to an owner or a technician profile and apply the ownership rules. A
 * request of another owner, or not assigned to the calling technician, is reported exactly like a
 * request that does not exist.</p>
 *
 * <p>Every endpoint declares its roles through method security, so an account outside them is
 * answered with 403 before a service is reached. Creating, assigning and canceling are
 * {@code OWNER_ROLE} only; accepting, rejecting and starting are {@code TECHNICIAN_ROLE} only; the
 * reads are open to both. An illegal move of the state machine is answered with
 * {@code 409 INVALID_STATUS_TRANSITION}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/service-requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Service Requests", description = "Maintenance and repair service request endpoints: creation, assignment, lifecycle and history")
public class ServiceRequestsController {

  private static final String SERVICE_REQUEST_RESOURCE = "Service_Request";
  private static final String REVIEW_RESOURCE = "Review";
  private static final String ASSIGNED_TO_ME = "me";

  private final ServiceRequestCommandService serviceRequestCommandService;
  private final ServiceRequestQueryService serviceRequestQueryService;
  private final ReviewQueryService reviewQueryService;

  /**
   * Constructor
   *
   * @param serviceRequestCommandService The {@link ServiceRequestCommandService} instance
   * @param serviceRequestQueryService   The {@link ServiceRequestQueryService} instance
   * @param reviewQueryService           The {@link ReviewQueryService} instance
   */
  public ServiceRequestsController(
      ServiceRequestCommandService serviceRequestCommandService,
      ServiceRequestQueryService serviceRequestQueryService,
      ReviewQueryService reviewQueryService) {
    this.serviceRequestCommandService = serviceRequestCommandService;
    this.serviceRequestQueryService = serviceRequestQueryService;
    this.reviewQueryService = reviewQueryService;
  }

  /**
   * Creates a service request for one of the authenticated owner's units.
   *
   * @param resource       The {@link CreateServiceRequestResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The created {@link ServiceRequestResource}, with 201 Created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  @Operation(
      summary = "Create a service request",
      description = "Creates a maintenance or repair request for a unit of the authenticated owner. "
          + "The request starts PENDING, without a technician.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Service request created",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<?> createServiceRequest(
      @Valid @RequestBody CreateServiceRequestResource resource,
      Authentication authentication) {
    var command = CreateServiceRequestCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        serviceRequestCommandService.handle(command),
        ServiceRequestResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Lists one page of the caller's service requests, newest first.
   *
   * @param status         Restrict to these statuses, comma-separated or repeated; optional
   * @param assignedTo     {@code me} for the requests assigned to the calling technician; optional
   * @param page           Zero-based page index; defaults to 0
   * @param size           Page size, from 1 to 100; defaults to 20
   * @param authentication The authenticated principal, whose name and role identify the caller
   * @return A {@link PagedServiceRequestResource}
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('OWNER_ROLE', 'TECHNICIAN_ROLE')")
  @Operation(
      summary = "List my service requests",
      description = "An owner lists the requests it created; a technician lists the requests assigned "
          + "to it, which assignedTo=me makes explicit. Optionally filtered by one or more statuses, "
          + "always paged and ordered by creation date, newest first.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Service requests found",
          content = @Content(schema = @Schema(implementation = PagedServiceRequestResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid filter or paging value, such as an unknown status, a page size above 100, or assignedTo=me sent by an owner"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician")
  })
  public ResponseEntity<?> listServiceRequests(
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these statuses, comma-separated", example = "PENDING,ACCEPTED")
      List<ServiceStatusResource> status,
      @RequestParam(required = false)
      @Parameter(description = "Only accepts 'me', for the requests assigned to the calling technician", example = "me")
      String assignedTo,
      @RequestParam(required = false)
      @Parameter(description = "Zero-based page index", example = "0")
      Integer page,
      @RequestParam(required = false)
      @Parameter(description = "Page size, from 1 to 100", example = "20")
      Integer size,
      Authentication authentication) {
    var query = new ListServiceRequestsQuery(
        authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication),
        toDomainStatuses(status),
        isAssignedToMe(assignedTo),
        page,
        size);
    return ResponseEntity.ok(ServiceRequestResourceFromEntityAssembler.toPagedResourceFromPage(
        serviceRequestQueryService.handle(query)));
  }

  /**
   * Fetches one of the caller's service requests with its status history.
   *
   * @param serviceRequestId The service request identifier
   * @param authentication   The authenticated principal, whose name and role identify the caller
   * @return The {@link ServiceRequestResource}, or 404 when it is not reachable by the caller
   */
  @GetMapping("/{serviceRequestId}")
  @PreAuthorize("hasAnyAuthority('OWNER_ROLE', 'TECHNICIAN_ROLE')")
  @Operation(
      summary = "Get a service request by ID",
      description = "Retrieves a request with its full status history. Only its owner and its "
          + "assigned technician can read it.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Service request found",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician"),
      @ApiResponse(responseCode = "404", description = "No such service request is reachable by the caller")
  })
  public ResponseEntity<?> getServiceRequestById(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      Authentication authentication) {
    var serviceRequest = serviceRequestQueryService.handle(new GetServiceRequestByIdQuery(
        serviceRequestId,
        authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication)));
    if (serviceRequest.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(SERVICE_REQUEST_RESOURCE, String.valueOf(serviceRequestId)));
    }
    return ResponseEntity.ok(ServiceRequestResourceFromEntityAssembler.toResourceFromEntity(serviceRequest.get()));
  }

  /**
   * Assigns a technician to one of the authenticated owner's service requests.
   *
   * @param serviceRequestId The service request identifier
   * @param resource         The {@link AssignTechnicianResource} payload
   * @param authentication   The authenticated principal, whose name identifies the caller
   * @return The updated {@link ServiceRequestResource}
   */
  @PutMapping(value = "/{serviceRequestId}/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  @Operation(
      summary = "Assign a technician",
      description = "Assigns a technician to a PENDING or ACCEPTED request, replacing the current one "
          + "if any. The request goes back to PENDING until the new technician accepts it.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Technician assigned",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such service request is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "TECHNICIAN_NOT_ELIGIBLE, INVALID_STATUS_TRANSITION or CONCURRENT_MODIFICATION_CONFLICT")
  })
  public ResponseEntity<?> assignTechnician(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      @Valid @RequestBody AssignTechnicianResource resource,
      Authentication authentication) {
    var command = AssignTechnicianCommandFromResourceAssembler.toCommandFromResource(
        serviceRequestId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        serviceRequestCommandService.handle(command),
        ServiceRequestResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Accepts a service request assigned to the authenticated technician.
   *
   * @param serviceRequestId The service request identifier
   * @param authentication   The authenticated principal, whose name identifies the caller
   * @return The updated {@link ServiceRequestResource}
   */
  @PutMapping("/{serviceRequestId}/accept")
  @PreAuthorize("hasAuthority('TECHNICIAN_ROLE')")
  @Operation(
      summary = "Accept a service request",
      description = "Accepts a PENDING request assigned to the authenticated technician.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Service request accepted",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not a technician"),
      @ApiResponse(responseCode = "404", description = "No such service request is assigned to the caller"),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION or CONCURRENT_MODIFICATION_CONFLICT")
  })
  public ResponseEntity<?> acceptServiceRequest(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        serviceRequestCommandService.handle(new AcceptServiceRequestCommand(serviceRequestId, authentication.getName())),
        ServiceRequestResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Rejects a service request assigned to the authenticated technician.
   *
   * @param serviceRequestId The service request identifier
   * @param resource         The {@link RejectServiceRequestResource} payload
   * @param authentication   The authenticated principal, whose name identifies the caller
   * @return The updated {@link ServiceRequestResource}
   */
  @PutMapping(value = "/{serviceRequestId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('TECHNICIAN_ROLE')")
  @Operation(
      summary = "Reject a service request",
      description = "Rejects a PENDING request assigned to the authenticated technician. The request "
          + "stays PENDING without a technician, so the owner can assign another one.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Service request rejected",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "400", description = "Missing reason"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not a technician"),
      @ApiResponse(responseCode = "404", description = "No such service request is assigned to the caller"),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION or CONCURRENT_MODIFICATION_CONFLICT")
  })
  public ResponseEntity<?> rejectServiceRequest(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      @Valid @RequestBody RejectServiceRequestResource resource,
      Authentication authentication) {
    var command = RejectServiceRequestCommandFromResourceAssembler.toCommandFromResource(
        serviceRequestId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        serviceRequestCommandService.handle(command),
        ServiceRequestResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Starts the work on a service request assigned to the authenticated technician.
   *
   * @param serviceRequestId The service request identifier
   * @param resource         The optional {@link StartServiceRequestResource} payload
   * @param authentication   The authenticated principal, whose name identifies the caller
   * @return The updated {@link ServiceRequestResource}
   */
  @PutMapping("/{serviceRequestId}/start")
  @PreAuthorize("hasAuthority('TECHNICIAN_ROLE')")
  @Operation(
      summary = "Start a service request",
      description = "Moves an ACCEPTED request assigned to the authenticated technician to IN_PROGRESS. "
          + "The body is optional and carries the start instant reported by the device; repeating the "
          + "call with the same instant is an idempotent no-op.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Service request started",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "400", description = "A start instant in the future"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not a technician"),
      @ApiResponse(responseCode = "404", description = "No such service request is assigned to the caller"),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION or CONCURRENT_MODIFICATION_CONFLICT")
  })
  public ResponseEntity<?> startServiceRequest(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      @RequestBody(required = false) StartServiceRequestResource resource,
      Authentication authentication) {
    var command = StartServiceRequestCommandFromResourceAssembler.toCommandFromResource(
        serviceRequestId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        serviceRequestCommandService.handle(command),
        ServiceRequestResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Cancels one of the authenticated owner's service requests.
   *
   * @param serviceRequestId The service request identifier
   * @param resource         The {@link CancelServiceRequestResource} payload
   * @param authentication   The authenticated principal, whose name identifies the caller
   * @return The updated {@link ServiceRequestResource}
   */
  @PutMapping(value = "/{serviceRequestId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  @Operation(
      summary = "Cancel a service request",
      description = "Cancels a PENDING or ACCEPTED request of the authenticated owner. A request whose "
          + "work already started, or that is already closed, cannot be canceled.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Service request canceled",
          content = @Content(schema = @Schema(implementation = ServiceRequestResource.class))),
      @ApiResponse(responseCode = "400", description = "Missing reason"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such service request is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION or CONCURRENT_MODIFICATION_CONFLICT")
  })
  public ResponseEntity<?> cancelServiceRequest(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      @Valid @RequestBody CancelServiceRequestResource resource,
      Authentication authentication) {
    var command = CancelServiceRequestCommandFromResourceAssembler.toCommandFromResource(
        serviceRequestId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        serviceRequestCommandService.handle(command),
        ServiceRequestResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  /**
   * Fetches the review of one of the caller's service requests.
   *
   * @param serviceRequestId The service request identifier
   * @param authentication   The authenticated principal, whose name and role identify the caller
   * @return The {@link ReviewResource}, or 404 when there is none the caller may read
   */
  @GetMapping("/{serviceRequestId}/review")
  @PreAuthorize("hasAnyAuthority('OWNER_ROLE', 'TECHNICIAN_ROLE')")
  @Operation(
      summary = "Get the review of a service request",
      description = "Retrieves the review of a request. Only the owner who wrote it and the technician "
          + "it is about can read it.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Review found",
          content = @Content(schema = @Schema(implementation = ReviewResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is neither an owner nor a technician"),
      @ApiResponse(responseCode = "404", description = "No review of that request is reachable by the caller")
  })
  public ResponseEntity<?> getServiceRequestReview(
      @PathVariable
      @Parameter(description = "Service request unique identifier", example = "42", required = true)
      Long serviceRequestId,
      Authentication authentication) {
    var review = reviewQueryService.handle(new GetReviewByServiceRequestIdQuery(
        serviceRequestId,
        authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication)));
    if (review.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(REVIEW_RESOURCE, "service request " + serviceRequestId));
    }
    return ResponseEntity.ok(ReviewResourceFromEntityAssembler.toResourceFromEntity(review.get()));
  }

  /**
   * Translates the status filter into its domain counterpart. An empty entry, as left by a trailing
   * comma, is ignored rather than failing the whole request.
   */
  private static Set<ServiceStatus> toDomainStatuses(List<ServiceStatusResource> statuses) {
    if (statuses == null) {
      return Set.of();
    }
    return statuses.stream()
        .filter(Objects::nonNull)
        .map(ServiceStatusResource::toDomain)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Reads the {@code assignedTo} parameter, which only accepts {@code me}.
   *
   * @throws IllegalArgumentException for any other value, answered with 400 by
   *                                  {@code GlobalExceptionHandler}
   */
  private static boolean isAssignedToMe(String assignedTo) {
    if (assignedTo == null || assignedTo.isBlank()) {
      return false;
    }
    if (ASSIGNED_TO_ME.equalsIgnoreCase(assignedTo.strip())) {
      return true;
    }
    throw new IllegalArgumentException("Parameter 'assignedTo' only accepts 'me'");
  }
}
