package pe.edu.upc.ice.track.platform.monitoring.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.SensorReadingCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.SensorReadingQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordReadingBatchResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.SensorReadingResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.RecordReadingBatchCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.SensorReadingResourceFromEntityAssembler;

import java.time.LocalDateTime;

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
@RequestMapping(value = "/api/v1/telemetry", produces = "application/json")
public class TelemetryController {

  private final SensorReadingCommandService sensorReadingCommandService;
  private final SensorReadingQueryService sensorReadingQueryService;

  public TelemetryController(
      SensorReadingCommandService sensorReadingCommandService,
      SensorReadingQueryService sensorReadingQueryService) {
    this.sensorReadingCommandService = sensorReadingCommandService;
    this.sensorReadingQueryService = sensorReadingQueryService;
  }

  @PostMapping
  public ResponseEntity<SensorReadingResource> recordReading(@RequestBody RecordReadingBatchResource resource) {
    var command = RecordReadingBatchCommandFromResourceAssembler.toCommandFromResource(resource);
    return sensorReadingCommandService.handle(command)
        .map(reading -> ResponseEntity.ok(SensorReadingResourceFromEntityAssembler.toResourceFromEntity(reading)))
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/equipment/{equipmentId}")
  public ResponseEntity<java.util.List<SensorReadingResource>> getReadingsByEquipment(
      @PathVariable Long equipmentId,
      @RequestParam LocalDateTime from,
      @RequestParam LocalDateTime to) {
    var readings = sensorReadingQueryService.handle(new GetReadingsByEquipmentQuery(equipmentId, from, to));
    var resources = readings.stream()
        .map(SensorReadingResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(resources);
  }
}
