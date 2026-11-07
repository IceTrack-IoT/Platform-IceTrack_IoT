package pe.edu.upc.ice.track.platform.notifications.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.profiles.ExternalProfilesServiceForNotifications;
import pe.edu.upc.ice.track.platform.notifications.application.queryservices.NotificationQueryService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.DismissNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.NotificationResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.transform.NotificationResourceFromEntityAssembler;

import java.util.List;

/**
 * REST controller for the authenticated caller's notification inbox: listing, detail and
 * read/dismiss transitions.
 *
 * <p>There is no creation endpoint: a notification is never created from a direct HTTP
 * request, only from an {@code AlertRaisedEventListener} (or a future equivalent) reacting to
 * an integration event from another bounded context.</p>
 *
 * <p>The recipient is always derived from the authenticated principal, never taken from the
 * request, the same way {@code DeviceTokensController} does. A notification addressed
 * to anybody else answers 404, exactly like a missing one, so identifiers cannot be harvested.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationsController {

  private final NotificationCommandService notificationCommandService;
  private final NotificationQueryService notificationQueryService;
  private final ExternalProfilesServiceForNotifications externalProfilesService;

  public NotificationsController(
      NotificationCommandService notificationCommandService,
      NotificationQueryService notificationQueryService,
      ExternalProfilesServiceForNotifications externalProfilesService) {
    this.notificationCommandService = notificationCommandService;
    this.notificationQueryService = notificationQueryService;
    this.externalProfilesService = externalProfilesService;
  }

  @GetMapping
  @Operation(
      summary = "Get my notifications",
      description = "Retrieves the authenticated caller's notifications, optionally filtered to "
          + "return only unread ones. The recipient is derived from the access token.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notifications retrieved successfully; empty when the caller has none",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<List<NotificationResource>> getMyNotifications(
      @Parameter(description = "Flag to filter only unread notifications")
      @RequestParam(defaultValue = "false") boolean onlyUnread,
      Authentication authentication) {
    var recipient = externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName());
    if (recipient.isEmpty()) {
      // Accounts without an owner profile (technicians today) receive no notifications yet.
      return ResponseEntity.ok(List.of());
    }
    var notifications = onlyUnread
        ? notificationQueryService.handle(new GetUnreadNotificationsByRecipientQuery(recipient.get()))
        : notificationQueryService.handle(new GetNotificationsByRecipientQuery(recipient.get()));
    return ResponseEntity.ok(notifications.stream().map(NotificationResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  @GetMapping("/{notificationId}")
  @Operation(
      summary = "Get notification by ID",
      description = "Retrieves one of the authenticated caller's notifications by its unique identifier.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notification found and retrieved successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "No such notification is addressed to the caller"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<NotificationResource> getNotificationById(
      @Parameter(description = "ID of the notification to retrieve", required = true)
      @PathVariable Long notificationId,
      Authentication authentication) {
    return externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName())
        .flatMap(recipient -> notificationQueryService.handle(new GetNotificationByIdQuery(notificationId, recipient)))
        .map(notification -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(notification)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{notificationId}/read")
  @Operation(
      summary = "Mark notification as read",
      description = "Marks one of the authenticated caller's notifications as read.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notification marked as read successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "No such notification is addressed to the caller"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<NotificationResource> markNotificationAsRead(
      @Parameter(description = "ID of the notification to mark as read", required = true)
      @PathVariable Long notificationId,
      Authentication authentication) {
    return externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName())
        .flatMap(recipient -> notificationCommandService.handle(new MarkNotificationAsReadCommand(notificationId, recipient)))
        .map(notification -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(notification)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{notificationId}/dismiss")
  @Operation(
      summary = "Dismiss notification",
      description = "Dismisses one of the authenticated caller's notifications from the active inbox.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notification dismissed successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "No such notification is addressed to the caller"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<NotificationResource> dismissNotification(
      @Parameter(description = "ID of the notification to dismiss", required = true)
      @PathVariable Long notificationId,
      Authentication authentication) {
    return externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName())
        .flatMap(recipient -> notificationCommandService.handle(new DismissNotificationCommand(notificationId, recipient)))
        .map(notification -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(notification)))
        .orElse(ResponseEntity.notFound().build());
  }
}
