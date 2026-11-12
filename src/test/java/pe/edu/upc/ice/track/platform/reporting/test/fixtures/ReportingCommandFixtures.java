package pe.edu.upc.ice.track.platform.reporting.test.fixtures;

import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting.ServiceIntervention;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.time.Instant;
import java.util.List;

import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

public final class ReportingCommandFixtures {

  public static final Instant CREATED = Instant.parse("2026-03-01T08:00:00Z");
  public static final Instant STARTED = Instant.parse("2026-03-01T09:00:00Z");
  public static final Instant COMPLETED = Instant.parse("2026-03-01T11:00:00Z");

  private ReportingCommandFixtures() {
  }

  public static RequestReportCommand uptimeCommand() {
    return new RequestReportCommand("  " + TITLE + "  ", ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME,
        SITE_ID, null, null, FROM, TO);
  }

  public static RequestReportCommand interventionCommand(Instant from, Instant to) {
    return new RequestReportCommand(TITLE, ReportType.SERVICE_INTERVENTION, ReportFormat.PDF, USERNAME,
        null, null, null, from, to, REQUEST_ID);
  }

  public static ServiceIntervention intervention(Long ownerId, String status) {
    return new ServiceIntervention(REQUEST_ID, ownerId, EQUIPMENT_ID, SITE_ID, TECHNICIAN_ID, "CORRECTIVE", "HIGH",
        status, "Compressor noise", CREATED, STARTED, COMPLETED,
        List.of(new ServiceIntervention.StatusChange("PENDING", "COMPLETED", COMPLETED, "done")),
        new ServiceIntervention.Work("Worn bearing", "Replaced bearing", List.of("bearing"), List.of("p1.jpg"),
            STARTED, COMPLETED),
        new ServiceIntervention.Rating(5, 4, 3, 4.0, "Good", COMPLETED));
  }
}
