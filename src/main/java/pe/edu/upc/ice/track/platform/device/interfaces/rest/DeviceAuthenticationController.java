package pe.edu.upc.ice.track.platform.device.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.device.interfaces.acl.DeviceContextFacade;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceAuthenticationRequest;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceAuthenticationResource;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

/** Internal HTTP adapter used by an Edge gateway to validate one device credential. */
@RestController
@RequestMapping(value = "/internal/v1/devices", produces = MediaType.APPLICATION_JSON_VALUE)
public class DeviceAuthenticationController {
  private final DeviceContextFacade deviceContextFacade;

  public DeviceAuthenticationController(DeviceContextFacade deviceContextFacade) {
    this.deviceContextFacade = deviceContextFacade;
  }

  @PostMapping(value = "/authenticate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> authenticate(
      @Valid @RequestBody DeviceAuthenticationRequest request,
      @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
    return deviceContextFacade.authenticate(request.deviceUid(), apiKey)
        .<ResponseEntity<?>>map(result -> ResponseEntity.ok(new DeviceAuthenticationResource(
            true, result.deviceId(), result.equipmentId())))
        .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
            ApplicationError.unauthorized("Invalid device uid or API key")));
  }
}
