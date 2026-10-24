package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;


import java.util.Objects;

/**
 *  Value object narrowing which rows a {@code ReportGenerationStrategy} reads, embedded in
 *  {@code Report} by composition -- it has no identity or lifecycle of its own.
 *
 *  <p>{@code siteId}, {@code equipmentId} and {@code technicianId} are each optional: which ones
 *  a given {@code ReportType} actually requires is enforced by its own
 *  {@code ReportGenerationStrategy}, not by this type, since the requirement differs per report
 *  (an equipment-scoped report needs {@code equipmentId}, a technician-scoped one needs
 *  {@code technicianId}, and so on). {@code technicianId} is not part of the original diagram's
 *  {@code ReportFilters} but is added here because {@code TechnicianPerformanceStrategy} cannot
 *  be computed without knowing which technician to score.</p>
 *
 * @param siteId       identifier of the site to scope by, or {@code null}
 * @param equipmentId  identifier of the equipment to scope by, or {@code null}
 * @param technicianId identifier of the technician to scope by, or {@code null}
 * @param dateRange    the window to compute the report over; required
 */
public record ReportFilters(SiteId siteId, EquipmentId equipmentId, TechnicianId technicianId, DateRange dateRange) {

  public ReportFilters {
    Objects.requireNonNull(dateRange, "dateRange must not be null");
  }
}
