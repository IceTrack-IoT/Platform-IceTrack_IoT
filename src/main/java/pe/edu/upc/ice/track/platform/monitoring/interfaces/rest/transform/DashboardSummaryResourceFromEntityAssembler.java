package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DashboardSummaryResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DashboardSummaryResource.AlertFiguresResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DashboardSummaryResource.DeviceFiguresResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DashboardSummaryResource.EquipmentFiguresResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DashboardSummaryResource.ServiceRequestFiguresResource;

import java.util.LinkedHashMap;

/** Maps a {@link DashboardSummary} read model into a {@link DashboardSummaryResource}. */
public final class DashboardSummaryResourceFromEntityAssembler {

  private DashboardSummaryResourceFromEntityAssembler() {
  }

  public static DashboardSummaryResource toResourceFromEntity(DashboardSummary summary) {
    var equipment = summary.equipment();
    var alerts = summary.alerts();
    var activeBySeverity = new LinkedHashMap<String, Long>();
    alerts.activeBySeverity().forEach((severity, count) -> activeBySeverity.put(severity.name(), count));
    return new DashboardSummaryResource(
        summary.generatedAt(),
        summary.siteCount(),
        new EquipmentFiguresResource(
            equipment.total(), equipment.byStatus(), equipment.reporting(), equipment.silent(),
            equipment.neverReported()),
        new AlertFiguresResource(alerts.open(), alerts.acknowledged(), activeBySeverity),
        new ServiceRequestFiguresResource(summary.serviceRequests().active(), summary.serviceRequests().byStatus()),
        new DeviceFiguresResource(summary.devices().paired(), summary.devices().revoked()));
  }
}
