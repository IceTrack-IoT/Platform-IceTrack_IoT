package pe.edu.upc.ice.track.platform.device.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
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
import pe.edu.upc.ice.track.platform.device.domain.model.commands.ChangeDeviceStatusCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RotateApiKeyCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.UnpairDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesByEquipmentQuery;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.ChangeDeviceStatusResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceCredentialResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceResource;
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

/**
 * REST API through which an owner pairs and manages the IoT boards of its refrigeration units.
 *
 * <p>Every endpoint here is called by the owner's web application with a user JWT. The boards
 * themselves never call these endpoints: they authenticate with their API key on the edge routes
 * ({@code POST /api/v1/telemetry}, {@code /api/v1/devices/me/**}).</p>
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
        DeviceCredentialResourceAssembler::toResourceFromResult,
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
        .<ResponseEntity<?>>map(device -> ResponseEntity.ok(DeviceResourceFromEntityAssembler.toResourceFromEntity(device)))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound("Device", String.valueOf(deviceId))));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "List the devices of an equipment unit",
      description = "Lists the devices linked to one of the caller's units; an empty list for any other unit.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Devices found",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviceResource.class)))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
      @ApiResponse(responseCode = "403", description = "Caller is not an owner")})
  public ResponseEntity<?> getDevicesByEquipment(@RequestParam("equipment_id") Long equipmentId,
                                                  Authentication authentication) {
    List<DeviceResource> resources = deviceQueryService
        .handle(new GetDevicesByEquipmentQuery(equipmentId, authentication.getName())).stream()
        .map(DeviceResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(resources);
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
        DeviceResourceFromEntityAssembler::toResourceFromEntity,
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
        DeviceCredentialResourceAssembler::toResourceFromResult,
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
        DeviceResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
