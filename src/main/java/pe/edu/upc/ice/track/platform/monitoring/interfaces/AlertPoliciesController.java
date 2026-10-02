package pe.edu.upc.ice.track.platform.monitoring.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertPolicyQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.UpdateAlertPolicyResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.AlertPolicyResourceFromEntityAssembler;

/** REST controller for creating and updating alert policies, global or per-equipment. */
@RestController
@RequestMapping(value = "/api/v1/alert-policies", produces = "application/json")
public class AlertPoliciesController {

  private final AlertPolicyCommandService alertPolicyCommandService;
  private final AlertPolicyQueryService alertPolicyQueryService;

  public AlertPoliciesController(
      AlertPolicyCommandService alertPolicyCommandService,
      AlertPolicyQueryService alertPolicyQueryService) {
    this.alertPolicyCommandService = alertPolicyCommandService;
    this.alertPolicyQueryService = alertPolicyQueryService;
  }

  @GetMapping("/equipment/{equipmentId}")
  public ResponseEntity<AlertPolicyResource> getPolicyByEquipment(@PathVariable Long equipmentId) {
    var policy = alertPolicyQueryService.handle(new GetAlertPolicyByEquipmentQuery(equipmentId));
    return ResponseEntity.ok(AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(policy));
  }

  @PutMapping
  public ResponseEntity<AlertPolicyResource> updatePolicy(@RequestBody UpdateAlertPolicyResource resource) {
    var command = new UpdateAlertPolicyCommand(
        resource.equipmentId(), resource.sustainedExcursionMinutes(),
        resource.hysteresisMarginCelsius(), resource.missedSyncWindowsForOffline());
    var policy = alertPolicyCommandService.handle(command);
    return ResponseEntity.ok(AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(policy));
  }
}
