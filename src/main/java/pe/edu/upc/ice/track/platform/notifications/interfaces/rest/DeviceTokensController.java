package pe.edu.upc.ice.track.platform.notifications.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.DeviceTokenCommandService;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.profiles.ExternalProfilesServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.RegisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.UnregisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.RegisterDeviceTokenResource;

/**
 * Lets a client app register or remove the FCM token of the device it runs on.
 *
 * <p>The recipient is always resolved from the authenticated principal, never from the request,
 * so a caller can only bind a device to itself.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/notifications/device-tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notification management endpoints")
public class DeviceTokensController {

  private final DeviceTokenCommandService deviceTokenCommandService;
  private final ExternalProfilesServiceForNotifications externalProfilesService;

  public DeviceTokensController(
      DeviceTokenCommandService deviceTokenCommandService,
      ExternalProfilesServiceForNotifications externalProfilesService) {
    this.deviceTokenCommandService = deviceTokenCommandService;
    this.externalProfilesService = externalProfilesService;
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Register device token",
      description = "Registers or refreshes the FCM token of the caller's device. Idempotent: call it on every app start and whenever FCM rotates the token.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Token registered"),
      @ApiResponse(responseCode = "400", description = "Invalid token or platform"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "The account has no owner profile")
  })
  public ResponseEntity<Void> registerDeviceToken(
      @Valid @RequestBody RegisterDeviceTokenResource resource, Authentication authentication) {
    var recipient = externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName());
    if (recipient.isEmpty()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    deviceTokenCommandService.handle(
        new RegisterDeviceTokenCommand(recipient.get(), resource.token(), resource.platform()));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  @Operation(
      summary = "Unregister device token",
      description = "Stops pushing to the given device. Call it on logout. Idempotent.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Token removed (or it was not registered)"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "The account has no owner profile")
  })
  public ResponseEntity<Void> unregisterDeviceToken(
      @Parameter(description = "FCM registration token to remove", required = true)
      @RequestParam String token,
      Authentication authentication) {
    var recipient = externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName());
    if (recipient.isEmpty()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    deviceTokenCommandService.handle(new UnregisterDeviceTokenCommand(recipient.get(), token));
    return ResponseEntity.noContent().build();
  }
}
