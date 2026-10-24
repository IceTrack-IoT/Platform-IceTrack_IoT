package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.RequestReportResource;

/**
 * Converts a {@link RequestReportResource} into a {@link RequestReportCommand}.
 */
public class RequestReportCommandFromResourceAssembler {

  private RequestReportCommandFromResourceAssembler() {
  }

  /**
   * @param resource the incoming resource
   * @return the equivalent command
   */
  public static RequestReportCommand toCommandFromResource(RequestReportResource resource) {
    return new RequestReportCommand(
        resource.title(),
        ReportType.valueOf(resource.type()),
        ReportFormat.valueOf(resource.format()),
        resource.userId(),
        resource.siteId(),
        resource.equipmentId(),
        resource.technicianId(),
        resource.from(),
        resource.to());
  }
}
