package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.TelemetryStreamCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.IssueStreamTicketCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RedeemStreamTicketCommand;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertStatusChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ConnectivityChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DeviceStatusChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.NotificationCreatedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ReadingRecordedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ReportStatusChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.UnreadCountChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.StreamTicketResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.StreamTicketResourceFromResultAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

/**
 * REST controller of the real-time telemetry stream: readings and alert status changes pushed to
 * the owner's dashboard over Server-Sent Events.
 *
 * <p>A browser's {@code EventSource} cannot send an {@code Authorization} header, so the stream is
 * opened in two steps. The owner first exchanges its JWT for a stream ticket
 * ({@code POST /stream-ticket}), then opens {@code GET /stream?ticket=...}. The ticket is valid for
 * 60 seconds and opens one stream at most, which limits the harm of it appearing in access logs.
 * The stream route is therefore exempt from the bearer token in the security configuration; the
 * ticket is its only credential.</p>
 *
 * <p>The stream carries only the units of the owner the ticket was issued for. Opening it touches no
 * database, so the long-lived request never holds a connection of the pool.</p>
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry Stream", description = "Real-time push of readings and alert status changes to the owner's dashboard")
@Slf4j
public class TelemetryStreamController {

  private final TelemetryStreamCommandService telemetryStreamCommandService;
  private final TelemetryStreamRegistry telemetryStreamRegistry;

  /**
   * Constructor
   *
   * @param telemetryStreamCommandService The {@link TelemetryStreamCommandService} instance
   * @param telemetryStreamRegistry       The {@link TelemetryStreamRegistry} the streams are opened on
   */
  public TelemetryStreamController(
      TelemetryStreamCommandService telemetryStreamCommandService,
      TelemetryStreamRegistry telemetryStreamRegistry) {
    this.telemetryStreamCommandService = telemetryStreamCommandService;
    this.telemetryStreamRegistry = telemetryStreamRegistry;
  }

  /**
   * Issues a stream ticket for the authenticated owner.
   *
   * @param authentication The authenticated principal, whose name identifies the owner
   * @return The ticket with 200 OK, never cached, or the reason it was refused
   */
  @PostMapping(value = "/stream-ticket", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(
      summary = "Issue a telemetry stream ticket",
      description = "Exchanges the owner's JWT for an opaque, single-use ticket valid for 60 seconds, to be passed "
          + "to GET /api/v1/telemetry/stream?ticket=... from a browser EventSource, which cannot send an "
          + "Authorization header. The ticket is bound to the authenticated owner. An owner may hold up to 10 "
          + "unredeemed tickets at once.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Ticket issued; the response carries Cache-Control: no-store",
          content = @Content(schema = @Schema(implementation = StreamTicketResource.class))),
      @ApiResponse(responseCode = "401", description = "UNAUTHORIZED - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "ACCESS_DENIED - the caller is not an owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS - too many unredeemed tickets; "
          + "Retry-After is 60",
          headers = @Header(name = HttpHeaders.RETRY_AFTER, description = "Seconds to wait before retrying",
              schema = @Schema(type = "integer")))
  })
  public ResponseEntity<?> issueStreamTicket(Authentication authentication) {
    var result = telemetryStreamCommandService.handle(new IssueStreamTicketCommand(authentication.getName()));
    return switch (result) {
      case Result.Success<IssuedStreamTicket, ApplicationError> success -> ResponseEntity.ok()
          .cacheControl(CacheControl.noStore())
          .body(StreamTicketResourceFromResultAssembler.toResourceFromResult(success.value()));
      case Result.Failure<IssuedStreamTicket, ApplicationError> failure -> {
        var response = ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        var builder = ResponseEntity.status(response.getStatusCode());
        if (response.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
          builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(IssuedStreamTicket.TTL.toSeconds()));
        }
        yield builder.body(response.getBody());
      }
    };
  }

  /**
   * Opens the event stream of the owner a ticket was issued for.
   *
   * <p>Every reading recorded and every alert raised or moved on one of the owner's units arrives as
   * a {@code telemetry-update} event, whose data carries {@code type} {@code READING_RECORDED} or
   * {@code ALERT_STATUS_CHANGED}. Notifications, report status changes, device lifecycle changes and
   * connectivity changes arrive under their own event names. A {@code stream-opened} event comes first,
   * then the events replayed after {@code last_event_id}, and a heartbeat comment every 25 seconds. The
   * stream closes after 30 minutes; the browser then needs a new ticket.</p>
   *
   * @param ticket      The stream ticket issued by {@code POST /stream-ticket}
   * @param lastEventId The id of the last event the client received, to replay what it missed; optional
   * @return The event stream, or 401 when the ticket is missing, unknown, used or expired
   */
  @GetMapping("/stream")
  @Operation(
      summary = "Open the owner event stream",
      description = "Server-Sent Events stream of the events of the ticket owner, and of no other owner. "
          + "Authenticated by the ticket query parameter only; no JWT is accepted here. The ticket is consumed by "
          + "this request whatever its outcome. Events: stream-opened first (reconnection delay, replayed and "
          + "replay_complete), then telemetry-update (a TelemetryReadingUpdate of type READING_RECORDED or a "
          + "TelemetryAlertUpdate of type ALERT_STATUS_CHANGED), notification-created, unread-count-changed, "
          + "report-status-changed, device-status-changed and connectivity-changed; a heartbeat comment every 25 "
          + "seconds. Every event but stream-opened carries an id. Pass the id of the last event received as "
          + "last_event_id when reconnecting: the events after it, kept for 5 minutes and up to 1000, are replayed "
          + "before the live ones; replay_complete false means some were lost and the client must reload over "
          + "REST. The stream closes after 30 minutes. An EventSource does not retry a 401: request a new ticket "
          + "and reconnect.")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Stream opened",
          content = @Content(
              mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
              schema = @Schema(oneOf = {
                  ReadingRecordedStreamResource.class,
                  AlertStatusChangedStreamResource.class,
                  NotificationCreatedStreamResource.class,
                  UnreadCountChangedStreamResource.class,
                  ReportStatusChangedStreamResource.class,
                  DeviceStatusChangedStreamResource.class,
                  ConnectivityChangedStreamResource.class}))),
      @ApiResponse(responseCode = "401", description = "UNAUTHORIZED - the ticket is missing, malformed, unknown, "
          + "already used or expired",
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<SseEmitter> openStream(
      @RequestParam(name = "ticket", required = false)
      @Parameter(description = "Stream ticket issued by POST /api/v1/telemetry/stream-ticket. Declared optional so "
          + "that a missing ticket is answered like any refused one: 401 UNAUTHORIZED, not 400",
          example = "st_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
      String ticket,
      @RequestParam(name = "last_event_id", required = false)
      @Parameter(description = "Id of the last event received on the previous stream, to replay what was missed. "
          + "A query parameter, because each reconnection builds a new EventSource with a new ticket and the "
          + "browser's Last-Event-ID header is never sent", example = "1731164560123-17")
      String lastEventId) {
    var ownerProfileId = telemetryStreamCommandService.handle(new RedeemStreamTicketCommand(ticket))
        .orElseThrow(InvalidStreamTicketException::new);
    var emitter = telemetryStreamRegistry.subscribe(ownerProfileId, lastEventId);
    log.info("Owner {} opened a telemetry stream", ownerProfileId);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .cacheControl(CacheControl.noCache())
        // Asks a buffering reverse proxy, such as nginx, to pass every event through at once.
        .header("X-Accel-Buffering", "no")
        .body(emitter);
  }

  /**
   * Answers a refused stream ticket with a JSON 401.
   *
   * <p>The stream endpoint returns an emitter, so its refusal travels as an exception. The content
   * type is set explicitly, because the browser asked for {@code text/event-stream}.</p>
   *
   * @param exception the refusal
   * @return the 401 response
   */
  @ExceptionHandler(InvalidStreamTicketException.class)
  public ResponseEntity<ErrorResource> handleInvalidStreamTicket(InvalidStreamTicketException exception) {
    var response = ErrorResponseAssembler.toErrorResponseFromApplicationError(
        ApplicationError.unauthorized("A valid, unused stream ticket is required"));
    return ResponseEntity.status(response.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(response.getBody());
  }

  /** Raised when the ticket presented to open the stream cannot be redeemed. */
  static final class InvalidStreamTicketException extends RuntimeException {
    InvalidStreamTicketException() {
      super("Invalid stream ticket", null, false, false);
    }
  }
}
