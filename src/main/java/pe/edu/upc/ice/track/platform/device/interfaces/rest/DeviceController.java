package pe.edu.upc.ice.track.platform.device.interfaces.rest;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.application.queryservices.DeviceQueryService;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetailsPage;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.ChangeDeviceStatusCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RotateApiKeyCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.UnpairDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.ChangeDeviceStatusResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceConnectivityStateResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceCredentialResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceStatusResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.RegisterDeviceResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.DeviceCredentialResourceAssembler;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.DeviceResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.RegisterDeviceCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST API through which an owner pairs and manages the IoT boards of its refrigeration units.
 *
 * <p>Every endpoint here is called by the owner's web application with a user JWT. The boards
 * themselves never call these endpoints: they authenticate with their API key on the edge routes
 * ({@code POST /api/v1/telemetry}, {@code /api/v1/devices/me/**}).</p>
 *
 * <p>Every {@link DeviceResource} answered here, by a read or by a change, names the unit the device
 * is linked to and the site of that unit. The listing answers a plain array by default and a page
 * wrapper only when the caller supplies both {@code page} and {@code size}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Devices", description = "IoT device pairing and lifecycle management endpoints")
public class DeviceController {
  private final DeviceCommandService deviceCommandService;
  private final DeviceQueryService deviceQueryService;

  public DeviceController(
      DeviceCommandService deviceCommandService,
      DeviceQueryService deviceQueryService) {
    this.deviceCommandService = deviceCommandService;
    this.deviceQueryService = deviceQueryService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Pair a device with an owned equipment unit",
      description = "Pairs an ESP32 board with one of the authenticated owner's units and issues its "
          + "API key (it_dev_...), returned only in this response and stored as a SHA-256 digest. "
          + "A board that was unpaired earlier is paired again under the same UID with a new key.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Device paired; api_key is shown only once",
          content = @Content(schema = @Schema(implementation = DeviceCredentialResource.class))),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "Equipment not found or not owned by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "409",
          description = "Device already paired, or the equipment already has a paired device",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))})
  public ResponseEntity<?> registerDevice(
      @Valid @RequestBody RegisterDeviceResource resource,
      Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        deviceCommandService.handle(RegisterDeviceCommandFromResourceAssembler
            .toCommandFromResource(authentication.getName(), resource)),
        this::toCredentialResource,
        HttpStatus.CREATED);
  }

  @GetMapping("/{deviceId}")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Get a device by ID",
      description = "Returns a device linked (paired or revoked) to one of the caller's units.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Device found",
          content = @Content(schema = @Schema(implementation = DeviceResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such device is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))})
  public ResponseEntity<?> getDevice(@PathVariable Long deviceId, Authentication authentication) {
    return deviceQueryService.handle(new GetDeviceByIdQuery(deviceId, authentication.getName()))
        .<ResponseEntity<?>>map(device -> ResponseEntity.ok(DeviceResourceFromEntityAssembler.toResourceFromDetails(device)))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound("Device", String.valueOf(deviceId))));
  }

  /**
   * Lists the devices linked to the caller's units: its whole inventory, or the part the filters
   * select.
   *
   * @param equipmentId    Restrict to the device of a single unit of the caller, optional
   * @param siteId         Restrict to the devices of the units of a single site of the caller, optional
   * @param status         Restrict to these statuses, comma-separated or repeated; optional
   * @param search         Case-insensitive text matched against the device uid or board model, optional
   * @param page           Zero-based page index; supply together with {@code size} to page
   * @param size           Page size; supply together with {@code page} to page
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return A plain {@link DeviceResource} array, or a page wrapper when paging was requested
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "List my devices",
      description = "Lists the devices linked (paired or revoked) to the units of the authenticated owner, "
          + "ordered by device_uid. Without a filter it answers the whole inventory in one read. It can be "
          + "narrowed to one unit, to the units of one site, to a set of statuses and by a search on the "
          + "device uid or board model; every filter combines with the others. An unpaired device belongs "
          + "to no owner, so status=UNPAIRED matches nothing. Each device carries the name and the site of "
          + "its unit, and its connectivity evaluated at read time; connectivity=SILENT,NEVER_REPORTED lists "
          + "only the boards in those states. Supplying both page and size returns a page wrapper with the "
          + "total count instead of a plain array; a size above 100 is capped to 100.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Devices found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviceResource.class)))),
      @ApiResponse(responseCode = "400",
          description = "VALIDATION_ERROR - unknown status or connectivity, search above 64 characters, negative page or size below 1",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
      @ApiResponse(responseCode = "404",
          description = "SITE_NOT_FOUND or EQUIPMENT_NOT_FOUND - the site or unit of a filter is not reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))})
  public ResponseEntity<?> getDevices(
      @RequestParam(name = "equipment_id", required = false)
      @Parameter(description = "Restrict to the device of a single unit of the caller", example = "21")
      Long equipmentId,
      @RequestParam(name = "site_id", required = false)
      @Parameter(description = "Restrict to the devices of the units of a single site of the caller", example = "1")
      Long siteId,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these statuses, comma-separated", example = "PAIRED,REVOKED")
      List<DeviceStatusResource> status,
      @RequestParam(required = false)
      @Parameter(description = "Case-insensitive search matching the device uid or board model", example = "ESP32")
      String search,
      @RequestParam(required = false)
      @Parameter(description = "Zero-based page index; supply with size", example = "0")
      Integer page,
      @RequestParam(required = false)
      @Parameter(description = "Page size, capped at 100; supply with page", example = "20")
      Integer size,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these connectivity states, evaluated at read time, comma-separated",
          example = "SILENT,NEVER_REPORTED")
      List<DeviceConnectivityStateResource> connectivity,
      Authentication authentication) {
    var query = new GetDevicesQuery(
        authentication.getName(), equipmentId, siteId, toDomainStatuses(status), search, page, size,
        toDomainConnectivityStates(connectivity));
    return ResponseEntityAssembler.<DeviceDetailsPage, Object>toResponseEntityFromResult(
        deviceQueryService.handle(query),
        devices -> query.isPaged()
            ? DeviceResourceFromEntityAssembler.toPagedResourceFromPage(devices)
            : DeviceResourceFromEntityAssembler.toResourcesFromDetails(devices.content()),
        HttpStatus.OK);
  }

  @PostMapping("/{deviceId}/unpair")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Unpair a device",
      description = "Unlinks a paired or revoked device from its unit and invalidates its API key. "
          + "Subsequent edge requests with that key receive 401; the unit and the board can both be "
          + "paired again.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Device unpaired",
          content = @Content(schema = @Schema(implementation = DeviceResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such device is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))})
  public ResponseEntity<?> unpairDevice(@PathVariable Long deviceId, Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        deviceCommandService.handle(new UnpairDeviceCommand(deviceId, authentication.getName())),
        this::toDeviceResource,
        HttpStatus.OK);
  }

  @PostMapping("/{deviceId}/rotate-api-key")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Rotate the API key of a device",
      description = "Issues a new API key, shown only in this response, and invalidates the previous "
          + "one. Also the way a revoked device resumes: it becomes PAIRED again.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "New API key issued",
          content = @Content(schema = @Schema(implementation = DeviceCredentialResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such device is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))})
  public ResponseEntity<?> rotateApiKey(@PathVariable Long deviceId, Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        deviceCommandService.handle(new RotateApiKeyCommand(deviceId, authentication.getName())),
        this::toCredentialResource,
        HttpStatus.OK);
  }

  @PutMapping(value = "/{deviceId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Change the lifecycle status of a device",
      description = "REVOKED invalidates the API key immediately while the device stays linked to its "
          + "unit; UNPAIRED also unlinks it. Moving a revoked device back to PAIRED is refused with "
          + "409: rotate its API key instead.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Status changed",
          content = @Content(schema = @Schema(implementation = DeviceResource.class))),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such device is reachable by the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "409", description = "Transition not allowed",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))})
  public ResponseEntity<?> changeStatus(@PathVariable Long deviceId,
                                        @Valid @RequestBody ChangeDeviceStatusResource resource,
                                        Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        deviceCommandService.handle(new ChangeDeviceStatusCommand(
            deviceId, authentication.getName(), resource.newStatus().toDomain())),
        this::toDeviceResource,
        HttpStatus.OK);
  }

  /**
   * Converts the device a command just changed, described with the unit it is linked to.
   */
  private DeviceResource toDeviceResource(Device device) {
    return DeviceResourceFromEntityAssembler.toResourceFromDetails(deviceQueryService.describe(device));
  }

  /**
   * Converts a credential issuance result, its device described with the unit it is linked to.
   */
  private DeviceCredentialResource toCredentialResource(DeviceCredentialResult result) {
    return DeviceCredentialResourceAssembler.toResourceFromResult(result, deviceQueryService.describe(result.device()));
  }

  /**
   * Translates the status filter into its domain counterpart. An empty entry, as left by a trailing
   * comma, is ignored rather than failing the whole request.
   */
  private static Set<DeviceStatus> toDomainStatuses(List<DeviceStatusResource> statuses) {
    if (statuses == null) {
      return Set.of();
    }
    return statuses.stream()
        .filter(Objects::nonNull)
        .map(DeviceStatusResource::toDomain)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Translates the connectivity filter into its domain counterpart, ignoring empty entries like the
   * status filter does.
   */
  private static Set<DeviceConnectivityState> toDomainConnectivityStates(List<DeviceConnectivityStateResource> states) {
    if (states == null) {
      return Set.of();
    }
    return states.stream()
        .filter(Objects::nonNull)
        .map(DeviceConnectivityStateResource::toDomain)
        .collect(Collectors.toUnmodifiableSet());
  }
}
