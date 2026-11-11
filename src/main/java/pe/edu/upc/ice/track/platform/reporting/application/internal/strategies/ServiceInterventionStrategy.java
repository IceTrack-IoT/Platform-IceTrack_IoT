package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting.ServiceIntervention;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportSection;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Realizes {@link ReportGenerationStrategy} for
 * {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType#SERVICE_INTERVENTION}:
 * the service document of one completed service request.
 *
 * <p>Scoped by {@code filters.serviceRequestId()}. The command service already checked, when the
 * report was requested, that the request is the requester's and {@code COMPLETED}; the request is read
 * again here, and a request that is no longer completed fails the report.</p>
 *
 * <p>The KPIs are the duration of the intervention, the spare parts and photos it lists and, when the
 * owner reviewed it, the three scores and their average. The document itself is in the sections: the
 * request, its status history, the intervention (diagnosis, actions taken, spare parts, photo links)
 * and the review.</p>
 */
@Service("SERVICE_INTERVENTION")
public class ServiceInterventionStrategy implements ReportGenerationStrategy {

  private final ExternalServiceRequestsServiceForReporting serviceRequestsService;
  private final ExternalProfilesServiceForReporting profilesService;
  private final ExternalAssetsServiceForReporting assetsService;

  public ServiceInterventionStrategy(
      ExternalServiceRequestsServiceForReporting serviceRequestsService,
      ExternalProfilesServiceForReporting profilesService,
      ExternalAssetsServiceForReporting assetsService) {
    this.serviceRequestsService = serviceRequestsService;
    this.profilesService = profilesService;
    this.assetsService = assetsService;
  }

  @Override
  public List<KpiValue> generate(ReportFilters filters) {
    var record = completedRequest(filters);
    var kpis = new ArrayList<KpiValue>();
    var work = record.work();
    if (work != null) {
      kpis.add(new KpiValue("intervention_duration_min", "Intervention duration",
          (double) Duration.between(work.startedAt(), work.completedAt()).toMinutes(), "min"));
      kpis.add(new KpiValue("spare_parts_count", "Spare parts used", (double) work.spareParts().size(), "count"));
      kpis.add(new KpiValue("photo_count", "Photos", (double) work.photoEvidence().size(), "count"));
    }
    var rating = record.rating();
    if (rating != null) {
      kpis.add(new KpiValue("review_average", "Review average", rating.average(), "stars"));
      kpis.add(new KpiValue("review_communication", "Communication", (double) rating.communication(), "stars"));
      kpis.add(new KpiValue("review_efficacy", "Efficacy", (double) rating.efficacy(), "stars"));
      kpis.add(new KpiValue("review_performance", "Performance", (double) rating.performance(), "stars"));
    }
    return kpis;
  }

  @Override
  public List<ReportSection> sections(ReportFilters filters) {
    var record = completedRequest(filters);
    var sections = new ArrayList<ReportSection>();
    sections.add(requestSection(record));
    sections.add(historySection(record));
    sections.add(interventionSection(record));
    sections.add(reviewSection(record));
    return sections;
  }

  /**
   * Reads the request the report documents and checks it is still completed.
   *
   * @param filters the report scope
   * @return the request
   * @throws IllegalStateException when the scope names no request, or the request no longer exists or
   *                               is not completed; the worker records the report as {@code FAILED}
   */
  private ServiceIntervention completedRequest(ReportFilters filters) {
    Objects.requireNonNull(filters, "filters must not be null");
    var serviceRequestId = filters.serviceRequestId();
    if (serviceRequestId == null) {
      throw new IllegalStateException("A service intervention report needs a service request");
    }
    var record = serviceRequestsService.fetchServiceIntervention(serviceRequestId)
        .orElseThrow(() -> new IllegalStateException("Service request %d no longer exists".formatted(serviceRequestId)));
    if (!record.isCompleted()) {
      throw new IllegalStateException("Service request %d is not completed".formatted(serviceRequestId));
    }
    return record;
  }

  private ReportSection requestSection(ServiceIntervention record) {
    var names = assetsService.fetchEquipmentAndSiteNames(record.equipmentId()).orElse(new String[] {null, null});
    var rows = new ArrayList<ReportSection.Row>();
    rows.add(new ReportSection.Row("Service request", "#" + record.serviceRequestId()));
    rows.add(new ReportSection.Row("Type", record.type()));
    rows.add(new ReportSection.Row("Priority", record.priority()));
    rows.add(new ReportSection.Row("Status", record.status()));
    rows.add(new ReportSection.Row("Equipment", describe(names[0], "#" + record.equipmentId())));
    rows.add(new ReportSection.Row("Site", describe(names[1], "#" + record.siteId())));
    rows.add(new ReportSection.Row("Technician", describe(
        profilesService.fetchTechnicianFullName(record.technicianProfileId()).orElse(null),
        record.technicianProfileId() == null ? null : "#" + record.technicianProfileId())));
    rows.add(new ReportSection.Row("Description", record.description()));
    rows.add(new ReportSection.Row("Created at", text(record.createdAt())));
    rows.add(new ReportSection.Row("Started at", text(record.startedAt())));
    rows.add(new ReportSection.Row("Completed at", text(record.completedAt())));
    return new ReportSection("Service request", rows);
  }

  private static ReportSection historySection(ServiceIntervention record) {
    var rows = record.history().stream()
        .map(change -> new ReportSection.Row(
            text(change.occurredAt()),
            (change.fromStatus() == null ? "Created as " + change.toStatus() : change.fromStatus() + " to " + change.toStatus())
                + (change.reason() == null ? "" : " - " + change.reason())))
        .toList();
    return new ReportSection("Status history", rows);
  }

  private static ReportSection interventionSection(ServiceIntervention record) {
    var work = record.work();
    if (work == null) {
      return new ReportSection("Intervention", List.of(new ReportSection.Row("Intervention", "Not registered")));
    }
    var rows = new ArrayList<ReportSection.Row>();
    rows.add(new ReportSection.Row("Started at", text(work.startedAt())));
    rows.add(new ReportSection.Row("Completed at", text(work.completedAt())));
    rows.add(new ReportSection.Row("Diagnosis", work.diagnosis()));
    rows.add(new ReportSection.Row("Actions taken", work.actionsTaken()));
    if (work.spareParts().isEmpty()) {
      rows.add(new ReportSection.Row("Spare parts", "None"));
    }
    for (var index = 0; index < work.spareParts().size(); index++) {
      rows.add(new ReportSection.Row("Spare part " + (index + 1), work.spareParts().get(index)));
    }
    if (work.photoEvidence().isEmpty()) {
      rows.add(new ReportSection.Row("Photos", "None"));
    }
    for (var index = 0; index < work.photoEvidence().size(); index++) {
      rows.add(new ReportSection.Row("Photo " + (index + 1), work.photoEvidence().get(index)));
    }
    return new ReportSection("Intervention", rows);
  }

  private static ReportSection reviewSection(ServiceIntervention record) {
    var rating = record.rating();
    if (rating == null) {
      return new ReportSection("Review", List.of(new ReportSection.Row("Review", "Not reviewed")));
    }
    return new ReportSection("Review", List.of(
        new ReportSection.Row("Average", String.format(Locale.ROOT, "%.2f / 5", rating.average())),
        new ReportSection.Row("Communication", rating.communication() + " / 5"),
        new ReportSection.Row("Efficacy", rating.efficacy() + " / 5"),
        new ReportSection.Row("Performance", rating.performance() + " / 5"),
        new ReportSection.Row("Comment", rating.comment() == null ? "-" : rating.comment()),
        new ReportSection.Row("Submitted at", text(rating.createdAt()))));
  }

  private static String describe(String name, String fallback) {
    if (name == null) {
      return fallback == null ? "-" : fallback;
    }
    return fallback == null ? name : name + " (" + fallback + ")";
  }

  private static String text(Instant instant) {
    return instant == null ? "-" : instant.toString();
  }
}
