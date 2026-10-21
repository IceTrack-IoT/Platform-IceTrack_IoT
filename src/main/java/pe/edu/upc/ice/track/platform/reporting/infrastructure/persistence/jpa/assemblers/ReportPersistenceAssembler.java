package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities.ReportPersistenceEntity;

/**
 * Converts between {@link Report} and {@link ReportPersistenceEntity}.
 *
 * <p>Rebuilding a {@link Report} from its entity goes through
 * {@code Report.rehydrate(...)} rather than its public constructor, so the aggregate's invariants
 * (e.g. a brand-new report always starting {@code PENDING}) are never re-run against already
 * persisted state.</p>
 */
public class ReportPersistenceAssembler {

  private ReportPersistenceAssembler() {
  }

  /**
   * @param report the aggregate to flatten
   * @return the equivalent persistence entity
   */
  public static ReportPersistenceEntity toEntity(Report report) {
    ReportFilters filters = report.getFilters();
    DateRange range = filters.dateRange();
    return new ReportPersistenceEntity(
        report.getId(),
        report.getTitle(),
        report.getType(),
        report.getFormat(),
        report.getStatus(),
        report.getResultPayload(),
        report.getUserId().userId(),
        filters.siteId() != null ? filters.siteId().siteId() : null,
        filters.equipmentId() != null ? filters.equipmentId().equipmentId() : null,
        filters.technicianId().technicianId(),
        range.from(),
        range.to(),
        report.getRequestedAt(),
        report.getCompletedAt());
  }

  /**
   * @param entity the persisted row to rebuild
   * @return the equivalent {@link Report} aggregate
   */
  public static Report toAggregate(ReportPersistenceEntity entity) {
    var filters = new ReportFilters(
        entity.getSiteId() != null ? new SiteId(entity.getSiteId()) : null,
        entity.getEquipmentId() != null ? new EquipmentId(entity.getEquipmentId()) : null,
        new TechnicianId(entity.getTechnicianId()),
        new DateRange(entity.getRangeFrom(), entity.getRangeTo()));

    return Report.rehydrate(
        entity.getId(),
        entity.getTitle(),
        entity.getType(),
        entity.getFormat(),
        entity.getStatus(),
        entity.getResultPayload(),
        new UserId(entity.getUserId()),
        filters,
        entity.getRequestedAt(),
        entity.getCompletedAt());
  }
}
