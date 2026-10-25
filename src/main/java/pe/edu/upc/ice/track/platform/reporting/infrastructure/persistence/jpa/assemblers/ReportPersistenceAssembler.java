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
        filters.technicianId() != null ? filters.technicianId().technicianId() : null,
        range.from(),
        range.to(),
        report.getRequestedAt(),
        report.getCompletedAt(),
        report.getFile() != null ? report.getFile().key() : null,
        report.getFile() != null ? report.getFile().contentType() : null,
        report.getFile() != null ? report.getFile().sizeBytes() : null);
  }

  /**
   * @param entity the persisted row to rebuild
   * @return the equivalent {@link Report} aggregate
   */
  public static Report toAggregate(ReportPersistenceEntity entity) {
    var filters = new ReportFilters(
        entity.getSiteId() != null ? new SiteId(entity.getSiteId()) : null,
        entity.getEquipmentId() != null ? new EquipmentId(entity.getEquipmentId()) : null,
        entity.getTechnicianId() != null ? new TechnicianId(entity.getTechnicianId()) : null,
        new DateRange(entity.getRangeFrom(), entity.getRangeTo()));

    var file = entity.getFileKey() != null
        ? new ReportFile(entity.getFileKey(), entity.getFileContentType(),
        entity.getFileSizeBytes() != null ? entity.getFileSizeBytes() : 0L)
        : null;

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
        entity.getCompletedAt(),
        file);
  }
}
