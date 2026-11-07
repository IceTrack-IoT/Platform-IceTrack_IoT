package pe.edu.upc.ice.track.platform.monitoring.interfaces;

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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.SensorReadingCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.SensorReadingQueryService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.ReadingIngestionOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordReadingBatchResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.SensorReadingResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.RecordReadingBatchCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.SensorReadingResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.infrastructure.documentation.openapi.configuration.OpenApiConfiguration;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller receiving aggregated reading batches from edge devices and exposing the
 * reading history to authenticated users.
 *
 * <p>{@code POST /api/v1/telemetry} is an edge route: it is authenticated by the device API key on
 * the edge filter chain of the Device Management context, never by a user JWT, and its caller
 * arrives here as an {@link AuthenticatedDevice}. The equipment and device the reading is stored
 * under are taken from that principal. The history endpoint stays on the user chain.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/telemetry", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry", description = "Sensor reading ingestion and history endpoints")
@Slf4j
public class TelemetryController {

  /** Message the edge contract specifies for every rejected reading batch. */
  static final String INVALID_READING_PAYLOAD = "Invalid reading payload";

  private final SensorReadingCommandService sensorReadingCommandService;
  private final SensorReadingQueryService sensorReadingQueryService;

  /**
   * Constructor
   * @param sensorReadingCommandService The {@link SensorReadingCommandService} instance
   * @param sensorReadingQueryService The {@link SensorReadingQueryService} instance
   */
  public TelemetryController(
      SensorReadingCommandService sensorReadingCommandService,
      SensorReadingQueryService sensorReadingQueryService) {
    this.sensorReadingCommandService = sensorReadingCommandService;
    this.sensorReadingQueryService = sensorReadingQueryService;
  }


  /**
   * Record an aggregated reading batch
   * @param resource The {@link RecordReadingBatchResource} instance sent by the edge device
   * @param device The device authenticated by its API key
   * @return 201 with the stored reading for a new batch, 200 with the stored reading for a batch
   *         already recorded under the same {@code readingUid}, 400 for an invalid payload, or 403
   *         when the payload names another device or equipment
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Record an aggregated reading batch",
      description = "Stores one aggregated sensor reading batch sent by an edge device, "
          + "de-duplicating by reading_uid. A batch never opens an alert - excursions are reported on "
          + "POST /api/v1/telemetry/events - but it resolves the active excursion alert once every batch of "
          + "the policy's sustained duration lies back within range by the hysteresis margin, as a fallback "
          + "for a lost normalization event. Authenticated by the X-API-Key header "
          + "only; the equipment and device are those the key is paired with. Raw temperatures "
          + "must lie within 0 to 50 °C, each calibrated temperature within 5 °C of its raw value, "
          + "humidity within 20 to 90 %, and min <= avg <= max for both sets. Light level, door-open "
          + "seconds and presence are optional; an omitted presence is stored as unavailable.",
      security = @SecurityRequirement(name = OpenApiConfiguration.DEVICE_API_KEY_SCHEME)
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "New batch recorded",
          content = @Content(schema = @Schema(implementation = SensorReadingResource.class))
      ),
      @ApiResponse(
          responseCode = "200",
          description = "Batch already recorded under this reading_uid; nothing was stored",
          content = @Content(schema = @Schema(implementation = SensorReadingResource.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid reading payload; nothing was stored",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Missing, unknown, revoked or malformed API key"),
      @ApiResponse(responseCode = "403", description = "equipment_id or device_id does not match the authenticated device",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "409", description = "reading_uid was already recorded by another device",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> recordReading(
      @Valid @RequestBody RecordReadingBatchResource resource,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedDevice device) {
    if (device == null) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.unauthorized("A device API key is required"));
    }
    if ((resource.deviceId() != null && !resource.deviceId().equals(device.deviceId()))
        || (resource.equipmentId() != null && !resource.equipmentId().equals(device.equipmentId()))) {
      log.warn("Rejected reading {} from device {}: payload names device {} and equipment {}",
          resource.readingUid(), device.deviceId(), resource.deviceId(), resource.equipmentId());
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.accessDenied("Payload identifiers do not match the authenticated device"));
    }
    var command = RecordReadingBatchCommandFromResourceAssembler.toCommandFromResource(resource, device);
    final ReadingIngestionOutcome outcome;
    try {
      outcome = sensorReadingCommandService.handle(command);
    } catch (IllegalArgumentException e) {
      return invalidReadingPayload(e.getMessage());
    }
    return switch (outcome) {
      case ReadingIngestionOutcome.Recorded recorded -> ResponseEntity.status(HttpStatus.CREATED)
          .body(SensorReadingResourceFromEntityAssembler.toResourceFromEntity(recorded.reading()));
      // A reading_uid stored by another device is not a resubmission of this one: answering it
      // with the stored reading would disclose another device's data.
      case ReadingIngestionOutcome.Duplicate duplicate
          when !duplicate.reading().getDeviceId().equals(device.deviceId()) ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.conflict(
              "Reading", "reading_uid is already used by another batch"));
      case ReadingIngestionOutcome.Duplicate duplicate ->
          ResponseEntity.ok(SensorReadingResourceFromEntityAssembler.toResourceFromEntity(duplicate.reading()));
    };
  }

  /**
   * Answers a reading batch that failed Bean Validation with the edge contract's
   * "Invalid reading payload" message, listing the offending fields in the details.
   *
   * <p>Only the ingestion endpoint has a validated body in this controller, so this handler never
   * applies to the history endpoint.</p>
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResource> handleInvalidReading(MethodArgumentNotValidException ex) {
    var details = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
        .collect(Collectors.joining("; "));
    return invalidReadingPayload(details.isEmpty() ? null : details);
  }

  /** Answers a body that is not well-formed JSON or holds a non-numeric value. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResource> handleUnreadableReading(HttpMessageNotReadableException ex) {
    return invalidReadingPayload("The request body is malformed or holds a non-numeric value");
  }

  private static ResponseEntity<ErrorResource> invalidReadingPayload(String details) {
    return ResponseEntity.badRequest().body(new ErrorResource("VALIDATION_ERROR", INVALID_READING_PAYLOAD, details));
  }

  /**
   * Get the reading history of equipment within a date range
   * @param equipmentId The equipment unique identifier
   * @param from Inclusive start of the range
   * @param to Inclusive end of the range
   * @param authentication The authenticated principal
   * @return A list of {@link SensorReadingResource} resources recorded within the range, or 404
   *         when the caller may not read the equipment
   */
  @GetMapping("/equipment/{equipmentId}")
  @PreAuthorize("hasAnyAuthority('" + RoleAuthorities.ROLE_OWNER + "', '" + RoleAuthorities.ROLE_TECHNICIAN + "')")
  @Operation(
      summary = "Get reading history by equipment",
      description = "Retrieves the sensor readings recorded for an equipment within the given "
          + "date range. Owners read their own units; technicians read units they hold an "
          + "active service request on. Any other unit answers 404.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Readings found",
          content = @Content(schema = @Schema(implementation = SensorReadingResource.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "404", description = "No such equipment is reachable by the caller")
  })
  public ResponseEntity<List<SensorReadingResource>> getReadingsByEquipment(
      @PathVariable
      @Parameter(description = "Equipment unique identifier", example = "1", required = true)
      Long equipmentId,
      @RequestParam
      @Parameter(description = "Inclusive start of the date range", required = true)
      LocalDateTime from,
      @RequestParam
      @Parameter(description = "Inclusive end of the date range", required = true)
      LocalDateTime to,
      Authentication authentication) {
    var query = new GetReadingsByEquipmentQuery(equipmentId, from, to, authentication.getName(),
        CallerRoleFromAuthenticationAssembler.toCallerRoleFromAuthentication(authentication));
    return sensorReadingQueryService.handle(query)
        .map(readings -> ResponseEntity.ok(readings.stream()
            .map(SensorReadingResourceFromEntityAssembler::toResourceFromEntity)
            .toList()))
        .orElse(ResponseEntity.notFound().build());
  }
}
