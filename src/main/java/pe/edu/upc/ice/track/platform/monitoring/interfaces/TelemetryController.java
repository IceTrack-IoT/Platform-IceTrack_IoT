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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.SensorReadingCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.SensorReadingQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordReadingBatchResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.SensorReadingResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.RecordReadingBatchCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.SensorReadingResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.CallerRoleFromAuthenticationAssembler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller receiving aggregated reading batches from the Edge API and exposing the
 * reading history to authenticated users.
 *
 * <p>Authentication for {@code POST /telemetry} is by device API key, not by user JWT — the
 * security filter chain routes this endpoint through a distinct chain from the rest of the
 * controllers in this bounded context (see the security configuration in the Edge API
 * bounded context).</p>
 */
@RestController
@RequestMapping(value = "/api/v1/telemetry", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry", description = "Sensor reading ingestion and history endpoints")
public class TelemetryController {

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
   * @param resource The {@link RecordReadingBatchResource} instance sent by the Edge API
   * @return A {@link SensorReadingResource} for the stored reading, or no content when the
   *         batch was a duplicate delivery already recorded under the same {@code readingUid}
   */
  @PostMapping
  @Operation(
      summary = "Record an aggregated reading batch",
      description = "Stores one aggregated sensor reading batch forwarded by the Edge API, "
          + "de-duplicating by readingUid, and triggers threshold evaluation against the "
          + "applicable alert policy for the equipment."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Reading recorded successfully",
          content = @Content(schema = @Schema(implementation = SensorReadingResource.class))
      ),
      @ApiResponse(responseCode = "204", description = "Duplicate delivery, reading already recorded"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  public ResponseEntity<SensorReadingResource> recordReading(@Valid @RequestBody RecordReadingBatchResource resource) {
    var command = RecordReadingBatchCommandFromResourceAssembler.toCommandFromResource(resource);
    return sensorReadingCommandService.handle(command)
        .map(reading -> ResponseEntity.ok(SensorReadingResourceFromEntityAssembler.toResourceFromEntity(reading)))
        .orElse(ResponseEntity.noContent().build());
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
