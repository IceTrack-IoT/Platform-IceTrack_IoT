package pe.edu.upc.ice.track.platform.monitoring.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertResourceFromEntityAssembler;

import java.util.List;

/** REST controller for alert listing, detail and lifecycle transitions (US-28, US-29). */
@RestController
@RequestMapping(value = "/api/v1/alerts", produces = "application/json")
public class AlertsController {

  private final AlertCommandService alertCommandService;
  private final AlertQueryService alertQueryService;

  public AlertsController(AlertCommandService alertCommandService, AlertQueryService alertQueryService) {
    this.alertCommandService = alertCommandService;
    this.alertQueryService = alertQueryService;
  }

  @GetMapping("/{alertId}")
  public ResponseEntity<AlertResource> getAlertById(@PathVariable Long alertId) {
    return alertQueryService.handle(new GetAlertByIdQuery(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/equipment/{equipmentId}/open")
  public ResponseEntity<List<AlertResource>> getOpenAlertsByEquipment(@PathVariable Long equipmentId) {
    var alerts = alertQueryService.handle(new GetOpenAlertsByEquipmentQuery(equipmentId));
    return ResponseEntity.ok(alerts.stream().map(AlertResourceFromEntityAssembler::toResourceFromEntity).toList());
  }

  @PutMapping("/{alertId}/acknowledge")
  public ResponseEntity<AlertResource> acknowledgeAlert(@PathVariable Long alertId) {
    return alertCommandService.handle(new AcknowledgeAlertCommand(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{alertId}/resolve")
  public ResponseEntity<AlertResource> resolveAlert(@PathVariable Long alertId) {
    return alertCommandService.handle(new ResolveAlertCommand(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{alertId}/dismiss")
  public ResponseEntity<AlertResource> dismissAlert(@PathVariable Long alertId) {
    return alertCommandService.handle(new DismissAlertCommand(alertId))
        .map(alert -> ResponseEntity.ok(AlertResourceFromEntityAssembler.toResourceFromEntity(alert)))
        .orElse(ResponseEntity.notFound().build());
  }
}
