package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.CreateReportResource;

import java.util.Locale;

/**
 * Converts a {@link CreateReportResource} into a {@link RequestReportCommand}.
 *
 * <p>A type or format that names no known value is passed on as {@code null}, which the command
 * service refuses with a validation error listing the accepted values.</p>
 */
public class RequestReportCommandFromResourceAssembler {

  private RequestReportCommandFromResourceAssembler() {
  }

  /**
   * @param resource the incoming resource
   * @param username username of the authenticated caller, the requester of the report
   * @return the equivalent command
   */
  public static RequestReportCommand toCommandFromResource(CreateReportResource resource, String username) {
    var filters = resource.filters();
    return new RequestReportCommand(
        resource.name(),
        parse(ReportType.class, resource.type()),
        parse(ReportFormat.class, resource.format()),
        username,
        filters.siteId(),
        filters.equipmentId(),
        filters.technicianId(),
        filters.from(),
        filters.to(),
        filters.serviceRequestId());
  }

  private static <E extends Enum<E>> E parse(Class<E> enumType, String value) {
    if (value == null) {
      return null;
    }
    try {
      return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
