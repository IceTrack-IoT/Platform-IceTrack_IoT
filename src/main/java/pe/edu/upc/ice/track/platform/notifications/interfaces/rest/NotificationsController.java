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
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
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
 * REST controller for notification listing, detail and read/dismiss transitions.
 *
 * <p>There is no creation endpoint: a notification is never created from a direct HTTP
 * request, only from an {@code AlertRaisedEventHandler} (or a future equivalent) reacting to
 * an integration event from another bounded context.</p>
 *
 * <p>NOTE: {@code recipientUserId} is taken as a request parameter pending JWT wiring, the same
 * simplification already flagged in Asset Management's controllers ({@code ownerId} hardcoded).
 * Once authentication is wired, it should be resolved from the authenticated principal instead.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationsController {

  private final NotificationCommandService notificationCommandService;
  private final NotificationQueryService notificationQueryService;

  public NotificationsController(
      NotificationCommandService notificationCommandService,
      NotificationQueryService notificationQueryService) {
    this.notificationCommandService = notificationCommandService;
    this.notificationQueryService = notificationQueryService;
  }

  @GetMapping
  @Operation(
      summary = "Get notifications by recipient",
      description = "Retrieves a list of notifications for a specific recipient, optionally filtered to return only unread ones.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notifications retrieved successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
  })
  public ResponseEntity<List<NotificationResource>> getNotificationsByRecipient(
      @Parameter(description = "ID of the notification recipient user", required = true)
      @RequestParam Long recipientUserId,
      @Parameter(description = "Flag to filter only unread notifications")
      @RequestParam(defaultValue = "false") boolean onlyUnread) {
    var notifications = onlyUnread
        ? notificationQueryService.handle(new GetUnreadNotificationsByRecipientQuery(recipientUserId))
        : notificationQueryService.handle(new GetNotificationsByRecipientQuery(recipientUserId));
    return ResponseEntity.ok(notifications.stream().map(NotificationResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  @GetMapping("/{notificationId}")
  @Operation(
      summary = "Get notification by ID",
      description = "Retrieves details of a specific notification by its unique identifier.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notification found and retrieved successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Notification not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
  })
  public ResponseEntity<NotificationResource> getNotificationById(
      @Parameter(description = "ID of the notification to retrieve", required = true)
      @PathVariable Long notificationId) {
    return notificationQueryService.handle(new GetNotificationByIdQuery(notificationId))
        .map(notification -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(notification)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{notificationId}/read")
  @Operation(
      summary = "Mark notification as read",
      description = "Updates the status of a specific notification to mark it as read.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notification marked as read successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Notification not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
  })
  public ResponseEntity<NotificationResource> markNotificationAsRead(
      @Parameter(description = "ID of the notification to mark as read", required = true)
      @PathVariable Long notificationId) {
    return notificationCommandService.handle(new MarkNotificationAsReadCommand(notificationId))
        .map(notification -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(notification)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{notificationId}/dismiss")
  @Operation(
      summary = "Dismiss notification",
      description = "Updates the status of a specific notification to dismiss it.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Notification dismissed successfully",
          content = @Content(schema = @Schema(implementation = NotificationResource.class))
      ),
      @ApiResponse(responseCode = "404", description = "Notification not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
  })
  public ResponseEntity<NotificationResource> dismissNotification(
      @Parameter(description = "ID of the notification to dismiss", required = true)
      @PathVariable Long notificationId) {
    return notificationCommandService.handle(new DismissNotificationCommand(notificationId))
        .map(notification -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(notification)))
        .orElse(ResponseEntity.notFound().build());
  }
}