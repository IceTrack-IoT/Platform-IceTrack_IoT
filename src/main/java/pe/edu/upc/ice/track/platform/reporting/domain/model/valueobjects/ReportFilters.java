package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;


import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.util.Objects;

/**
 *  Value object narrowing which rows a {@code ReportGenerationStrategy} reads, embedded in
 *  {@code Report} by composition -- it has no identity or lifecycle of its own.
 *
 *  <p>{@code siteId}, {@code equipmentId}, {@code technicianId} and {@code serviceRequestId} are each
 *  optional: which ones a given {@code ReportType} actually requires is enforced by its own
 *  {@code ReportGenerationStrategy}, not by this type, since the requirement differs per report
 *  (an equipment-scoped report needs {@code equipmentId}, a technician-scoped one needs
 *  {@code technicianId}, a service document needs {@code serviceRequestId}, and so on).
 *  {@code technicianId} is not part of the original diagram's {@code ReportFilters} but is added
 *  here because {@code TechnicianPerformanceStrategy} cannot be computed without knowing which
 *  technician to score.</p>
 *
 * @param siteId           identifier of the site to scope by, or {@code null}
 * @param equipmentId      identifier of the equipment to scope by, or {@code null}
 * @param technicianId     identifier of the technician to scope by, or {@code null}
 * @param dateRange        the window to compute the report over; required
 * @param serviceRequestId identifier of the service request a service document is about, or
 *                         {@code null}
 */
public record ReportFilters(
    SiteId siteId, EquipmentId equipmentId, TechnicianId technicianId, DateRange dateRange, Long serviceRequestId) {

  public ReportFilters {
    Objects.requireNonNull(dateRange, "dateRange must not be null");
    if (serviceRequestId != null && serviceRequestId <= 0) {
      throw new IllegalArgumentException("serviceRequestId must be positive");
    }
  }

  /**
   * Creates filters that name no service request.
   */
  public ReportFilters(SiteId siteId, EquipmentId equipmentId, TechnicianId technicianId, DateRange dateRange) {
    this(siteId, equipmentId, technicianId, dateRange, null);
  }
}
