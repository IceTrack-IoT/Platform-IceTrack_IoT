package pe.edu.upc.ice.track.platform.device.interfaces.rest;

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
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.RegisterDeviceResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.DeviceCredentialResourceAssembler;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.DeviceResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.RegisterDeviceCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;

/** REST API for provisioning and managing the IoT boards of an owner. */
@RestController
@RequestMapping(value = "/api/v1/devices", produces = MediaType.APPLICATION_JSON_VALUE)
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
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
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
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  public ResponseEntity<?> getDevice(@PathVariable Long deviceId, Authentication authentication) {
    return deviceQueryService.handle(new GetDeviceByIdQuery(deviceId, authentication.getName()))
        .<ResponseEntity<?>>map(device -> ResponseEntity.ok(DeviceResourceFromEntityAssembler.toResourceFromEntity(device)))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.notFound("Device", String.valueOf(deviceId))));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  public ResponseEntity<?> getDevicesByEquipment(@RequestParam("equipment_id") Long equipmentId,
                                                  Authentication authentication) {
    List<DeviceResource> resources = deviceQueryService
        .handle(new GetDevicesByEquipmentQuery(equipmentId, authentication.getName())).stream()
        .map(DeviceResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(resources);
  }

  @PostMapping("/{deviceId}/unpair")
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  public ResponseEntity<?> unpairDevice(@PathVariable Long deviceId, Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        deviceCommandService.handle(new UnpairDeviceCommand(deviceId, authentication.getName())),
        DeviceResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }

  @PostMapping("/{deviceId}/rotate-api-key")
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  public ResponseEntity<?> rotateApiKey(@PathVariable Long deviceId, Authentication authentication) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        deviceCommandService.handle(new RotateApiKeyCommand(deviceId, authentication.getName())),
        DeviceCredentialResourceAssembler::toResourceFromResult,
        HttpStatus.OK);
  }

  @PutMapping(value = "/{deviceId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
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
