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
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationCountQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.NotificationResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.PagedNotificationResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.UnreadNotificationCountResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.transform.NotificationResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;

/**
 * REST controller for the authenticated caller's notification inbox: listing, detail and
 * read/dismiss transitions.
 *
 * <p>There is no creation endpoint: a notification is never created from a direct HTTP
 * request, only by an inbound listener reacting to an integration event from another bounded
 * context (alerts and maintenance windows, service request assignments, rejections and
 * cancellations). Owners and technicians each read their own inbox.</p>
 *
 * <p>The recipient is always derived from the authenticated principal, never taken from the
 * request, the same way {@code DeviceTokensController} does. A notification addressed
 * to anybody else answers 404, exactly like a missing one, so identifiers cannot be harvested.</p>
 *
 * <p>The inbox is paginated and only lists active notifications: a dismissed notification is
 * hidden, but kept. {@code /unread-count} feeds the unread badge.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationsController {

  /** Placeholder recipient used only to validate the paging values of a caller who has no inbox. */
  private static final Long NO_RECIPIENT = 0L;

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
      summary = "List my notifications",
      description = "Returns one page of the authenticated caller's active (not dismissed) notifications, most "
          + "recent first, optionally only the unread ones. The recipient is derived from the access token. "
          + "size defaults to 20 and is capped at 100.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Page of notifications; empty when the caller has none",
          content = @Content(schema = @Schema(implementation = PagedNotificationResource.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "VALIDATION_ERROR - a negative page or a size below 1",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<PagedNotificationResource> listMyNotifications(
      @Parameter(description = "Zero-based page index", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size, from 1 to 100; larger values are capped at 100", example = "20")
      @RequestParam(defaultValue = "" + GetNotificationsByRecipientQuery.DEFAULT_PAGE_SIZE) int size,
      @Parameter(description = "Return only notifications not yet read", example = "false")
      @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
      Authentication authentication) {
    var recipient = externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName());
    if (recipient.isEmpty()) {
      // An account without an owner or technician profile has no inbox. The paging values are still
      // validated, so the answer does not depend on whether the caller has one.
      var emptyQuery = new GetNotificationsByRecipientQuery(NO_RECIPIENT, unreadOnly, page, size);
      return ResponseEntity.ok(NotificationResourceFromEntityAssembler.toPagedResourceFromPage(
          NotificationPage.empty(emptyQuery.page(), emptyQuery.size())));
    }
    var query = new GetNotificationsByRecipientQuery(recipient.get(), unreadOnly, page, size);
    var notifications = notificationQueryService.handle(query);
    return ResponseEntity.ok(NotificationResourceFromEntityAssembler.toPagedResourceFromPage(notifications));
  }

  @GetMapping("/unread-count")
  @Operation(
      summary = "Count my unread notifications",
      description = "Returns how many of the authenticated caller's notifications are neither read nor dismissed, "
          + "for the unread badge. The recipient is derived from the access token.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Unread count; 0 when the caller has none",
          content = @Content(schema = @Schema(implementation = UnreadNotificationCountResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<UnreadNotificationCountResource> countMyUnreadNotifications(Authentication authentication) {
    var unreadCount = externalProfilesService.fetchRecipientUserIdByUsername(authentication.getName())
        .map(recipient -> notificationQueryService.handle(new GetUnreadNotificationCountQuery(recipient)))
        .orElse(0L);
    return ResponseEntity.ok(new UnreadNotificationCountResource(unreadCount));
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
