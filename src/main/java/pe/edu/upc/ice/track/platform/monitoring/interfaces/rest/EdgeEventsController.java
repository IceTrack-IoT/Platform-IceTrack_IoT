package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.EdgeEventCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.EdgeEventOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventResult;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.EdgeEventResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordEdgeEventResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.EdgeEventAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;
import pe.edu.upc.ice.track.platform.shared.infrastructure.documentation.openapi.configuration.OpenApiConfiguration;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.util.EnumSet;
import java.util.Set;

/**
 * REST controller receiving the events edge gateways report.
 *
 * <p>An edge route: authenticated by the device API key on the edge filter chain, never by a user
 * JWT. Kept apart from {@code TelemetryController}, whose error handlers answer every invalid body
 * with the reading-batch contract message.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/telemetry/events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry", description = "Sensor reading ingestion and history endpoints")
@Slf4j
public class EdgeEventsController {

  /** Outcomes that opened an alert or a maintenance window, or closed a window, answered 201. */
  private static final Set<EdgeEventResult> CREATING_RESULTS = EnumSet.of(
      EdgeEventResult.ALERT_OPENED,
      EdgeEventResult.MAINTENANCE_WINDOW_OPENED,
      EdgeEventResult.MAINTENANCE_WINDOW_CLOSED);

  private final EdgeEventCommandService edgeEventCommandService;

  public EdgeEventsController(EdgeEventCommandService edgeEventCommandService) {
    this.edgeEventCommandService = edgeEventCommandService;
  }

  /**
   * Records an edge event.
   *
   * @param resource the event
   * @param device   the device authenticated by its API key
   * @return 201 when a new alert or maintenance window was opened or a window was closed, 200
   *         otherwise, 409 when the event_uid belongs to another device
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Record an edge event",
      description = "Records an event detected by the gateway, idempotently by event_uid. "
          + "THERMAL_EXCURSION_DETECTED opens a CRITICAL alert dated at occurred_at (201), unless the unit "
          + "already has an OPEN or ACKNOWLEDGED one (200, ALERT_ALREADY_ACTIVE) or a maintenance window covers "
          + "occurred_at (200, SUPPRESSED_BY_MAINTENANCE). THERMAL_EXCURSION_NORMALIZED resolves the active alert, "
          + "recording the peak temperature and total duration (200, ALERT_RESOLVED), or changes nothing when none "
          + "is active (200, NO_ACTIVE_ALERT). MAINTENANCE_WINDOW_STARTED (requires origin) opens the unit's "
          + "maintenance window dated at occurred_at (201, MAINTENANCE_WINDOW_OPENED); while one is active, an APP "
          + "start takes over a BUTTON window (200, MAINTENANCE_WINDOW_PROMOTED) and any other start keeps it "
          + "(200, MAINTENANCE_ALREADY_ACTIVE). MAINTENANCE_WINDOW_ENDED (requires closure_reason) closes the "
          + "window named by start_event_uid, or the active one (201, MAINTENANCE_WINDOW_CLOSED), or changes "
          + "nothing (200, NO_ACTIVE_MAINTENANCE). A resent event_uid answers 200 with the stored outcome. Reading "
          + "batches never open alerts; only these events do. Authenticated by the X-API-Key header only.",
      security = @SecurityRequirement(name = OpenApiConfiguration.DEVICE_API_KEY_SCHEME))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "New alert or maintenance window opened, or window closed",
          content = @Content(schema = @Schema(implementation = EdgeEventResource.class))),
      @ApiResponse(responseCode = "200", description = "Event processed without creating anything, or already "
          + "recorded",
          content = @Content(schema = @Schema(implementation = EdgeEventResource.class))),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR - a missing field, an unknown type, a "
          + "detection without temperature_celsius, a window start without origin, a window end without "
          + "closure_reason, or a temperature outside -50 to 60 °C",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing, unknown, revoked or malformed API key"),
      @ApiResponse(responseCode = "403", description = "equipment_id or device_id does not match the authenticated device",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "409", description = "event_uid was already recorded by another device",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> recordEvent(
      @Valid @RequestBody RecordEdgeEventResource resource,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedDevice device) {
    if (device == null) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.unauthorized("A device API key is required"));
    }
    if ((resource.deviceId() != null && !resource.deviceId().equals(device.deviceId()))
        || (resource.equipmentId() != null && !resource.equipmentId().equals(device.equipmentId()))) {
      log.warn("Rejected edge event {} from device {}: payload names device {} and equipment {}",
          resource.eventUid(), device.deviceId(), resource.deviceId(), resource.equipmentId());
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.accessDenied("Payload identifiers do not match the authenticated device"));
    }
    final EdgeEventOutcome outcome;
    try {
      outcome = edgeEventCommandService.handle(EdgeEventAssembler.toCommandFromResource(resource, device));
    } catch (IllegalArgumentException e) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.validationError("edge-event", e.getMessage()));
    }
    return switch (outcome) {
      // An event_uid stored by another device is not a resubmission of this one: answering it with
      // the stored record would disclose another device's data.
      case EdgeEventOutcome.Duplicate duplicate when !duplicate.event().getDeviceId().equals(device.deviceId()) ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.conflict(
              "Edge_Event", "event_uid is already used by another event"));
      case EdgeEventOutcome.Duplicate duplicate ->
          ResponseEntity.ok(EdgeEventAssembler.toResourceFromEntity(duplicate.event()));
      case EdgeEventOutcome.Processed processed -> ResponseEntity
          .status(CREATING_RESULTS.contains(processed.event().getResult()) ? HttpStatus.CREATED : HttpStatus.OK)
          .body(EdgeEventAssembler.toResourceFromEntity(processed.event()));
    };
  }
}
